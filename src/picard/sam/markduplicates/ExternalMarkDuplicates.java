package picard.sam.markduplicates;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import htsjdk.samtools.DuplicateScoringStrategy;
import htsjdk.samtools.DuplicateScoringStrategy.ScoringStrategy;
import htsjdk.samtools.ReservedTagConstants;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMTag;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.ProgressLogger;
import htsjdk.samtools.util.SortingCollection;
import htsjdk.samtools.util.SortingLongCollection;
import picard.cmdline.CommandLineProgramProperties;
import picard.cmdline.Option;
import picard.cmdline.programgroups.SamOrBam;
import picard.sam.DuplicationMetrics;
import picard.sam.markduplicates.util.AbstractMarkDuplicatesCommandLineProgram;
import picard.sam.markduplicates.util.DiskBasedReadEndsForMarkDuplicatesMap;
import picard.sam.markduplicates.util.LibraryIdGenerator;
import picard.sam.markduplicates.util.ReadEnds;
import picard.sam.markduplicates.util.ReadEndsForMarkDuplicates;
import picard.sam.markduplicates.util.ReadEndsForMarkDuplicatesCodec;
import picard.sam.markduplicates.util.ReadEndsForMarkDuplicatesMap;
import picard.sam.markduplicates.util.ReadEndsForMarkDuplicatesWithBarcodes;
import picard.sam.markduplicates.util.ReadEndsForMarkDuplicatesWithBarcodesCodec;

@CommandLineProgramProperties(
        usage = "Examines aligned records in the supplied SAM or BAM file to locate duplicate molecules. " +
                "All records are then written to the output file with the duplicate records flagged.",
        usageShort = "Examines aligned records in the supplied SAM or BAM file to locate duplicate molecules.",
        programGroup = SamOrBam.class
)
public class ExternalMarkDuplicates extends AbstractMarkDuplicatesCommandLineProgram{


    private final Log log = Log.getInstance(MarkDuplicates.class);

    /**
     * If more than this many sequences in SAM file, don't spill to disk because there will not
     * be enough file handles.
     */

    @Option(shortName = "MAX_SEQS",
            doc = "This option is obsolete. ReadEnds will always be spilled to disk.")
    public int MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP = 50000;

    @Option(shortName = "MAX_FILE_HANDLES",
            doc = "Maximum number of file handles to keep open when spilling read ends to disk. " +
                    "Set this number a little lower than the per-process maximum number of file that may be open. " +
                    "This number can be found by executing the 'ulimit -n' command on a Unix system.")
    public int MAX_FILE_HANDLES_FOR_READ_ENDS_MAP = 8000;

    @Option(doc = "This number, plus the maximum RAM available to the JVM, determine the memory footprint used by " +
            "some of the sorting collections.  If you are running out of memory, try reducing this number.")
    public double SORTING_COLLECTION_SIZE_RATIO = 0.25;

    @Option(doc = "Barcode SAM tag (ex. BC for 10X Genomics)", optional = true)
    public String BARCODE_TAG = null;

    @Option(doc = "Read one barcode SAM tag (ex. BX for 10X Genomics)", optional = true)
    public String READ_ONE_BARCODE_TAG = null;

    @Option(doc = "Read two barcode SAM tag (ex. BX for 10X Genomics)", optional = true)
    public String READ_TWO_BARCODE_TAG = null;

    private SortingCollection<ReadEndsForMarkDuplicates> pairSort;
    private SortingCollection<ReadEndsForMarkDuplicates> fragSort;
    private SortingLongCollection duplicateIndexes;
    private int numDuplicateIndices = 0;

    protected LibraryIdGenerator libraryIdGenerator = null; // this is initialized in buildSortedReadEndLists

    private int getBarcodeValue(final SAMRecord record) {
        return EstimateLibraryComplexity.getReadBarcodeValue(record, BARCODE_TAG);
    }

    private int getReadOneBarcodeValue(final SAMRecord record) {
        return EstimateLibraryComplexity.getReadBarcodeValue(record, READ_ONE_BARCODE_TAG);
    }

    private int getReadTwoBarcodeValue(final SAMRecord record) {
        return EstimateLibraryComplexity.getReadBarcodeValue(record, READ_TWO_BARCODE_TAG);
    }

    public ExternalMarkDuplicates() {
        DUPLICATE_SCORING_STRATEGY = ScoringStrategy.SUM_OF_BASE_QUALITIES;
    }

    /** Stock main method. */
    public static void main(final String[] args) {
        new ExternalMarkDuplicates().instanceMainWithExit(args);
    }
    
    long index = 0;

    /**
     * Main work method.  Reads the BAM file once and collects sorted information about
     * the 5' ends of both ends of each read (or just one end in the case of pairs).
     * Then makes a pass through those determining duplicates before re-reading the
     * input file and writing it out with duplication flags set correctly.
     */
    protected int doWork() {
    	for(String file:INPUT){
    		IOUtil.assertFileIsReadable(new File(file));
    	}
        IOUtil.assertFileIsWritable(OUTPUT);
        IOUtil.assertFileIsWritable(METRICS_FILE);

        final boolean useBarcodes = (null != BARCODE_TAG || null != READ_ONE_BARCODE_TAG || null != READ_TWO_BARCODE_TAG);

        reportMemoryStats("Start of doWork");
        log.info("Reading input file and constructing read end information.");
        buildSortedReadEndLists(useBarcodes);
        reportMemoryStats("After buildSortedReadEndLists");
        generateDuplicateIndexes(useBarcodes);
        reportMemoryStats("After generateDuplicateIndexes");
        log.info("Marking " + this.numDuplicateIndices + " records as duplicates.");

        if (this.READ_NAME_REGEX == null) {
            log.warn("Skipped optical duplicate cluster discovery; library size estimation may be inaccurate!");
        } else {
            log.info("Found " + (this.libraryIdGenerator.getNumberOfOpticalDuplicateClusters()) + " optical duplicate clusters.");
        }

        final SamHeaderAndIterator headerAndIterator = openInputs();
        final SAMFileHeader header = headerAndIterator.header;

        final SAMFileHeader outputHeader = header.clone();
        outputHeader.setSortOrder(SAMFileHeader.SortOrder.coordinate);
        for (final String comment : COMMENT) outputHeader.addComment(comment);

        // Key: previous PG ID on a SAM Record (or null).  Value: New PG ID to replace it.
        final Map<String, String> chainedPgIds = getChainedPgIds(outputHeader);

        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(outputHeader,
                true,
                OUTPUT);

        // Now copy over the file while marking all the necessary indexes as duplicates
        long recordInFileIndex = 0;
        long nextDuplicateIndex = (this.duplicateIndexes.hasNext() ? this.duplicateIndexes.next() : -1);

        final ProgressLogger progress = new ProgressLogger(log, (int) 1e7, "Written");
        final CloseableIterator<SAMRecord> iterator = headerAndIterator.iterator;
        while (iterator.hasNext()) {
            final SAMRecord rec = iterator.next();
            if (!rec.isSecondaryOrSupplementary()) {
                final String library = LibraryIdGenerator.getLibraryName(header, rec);
                DuplicationMetrics metrics = libraryIdGenerator.getMetricsByLibrary(library);
                if (metrics == null) {
                    metrics = new DuplicationMetrics();
                    metrics.LIBRARY = library;
                    libraryIdGenerator.addMetricsByLibrary(library, metrics);
                }

                // First bring the simple metrics up to date
                if (rec.getReadUnmappedFlag()) {
                    ++metrics.UNMAPPED_READS;
                } else if (!rec.getReadPairedFlag() || rec.getMateUnmappedFlag()) {
                    ++metrics.UNPAIRED_READS_EXAMINED;
                } else {
                    ++metrics.READ_PAIRS_EXAMINED; // will need to be divided by 2 at the end
                }


                if (recordInFileIndex == nextDuplicateIndex) {
                    rec.setDuplicateReadFlag(true);

                    // Update the duplication metrics
                    if (!rec.getReadPairedFlag() || rec.getMateUnmappedFlag()) {
                        ++metrics.UNPAIRED_READ_DUPLICATES;
                    } else {
                        ++metrics.READ_PAIR_DUPLICATES;// will need to be divided by 2 at the end
                    }

                    // Now try and figure out the next duplicate index
                    if (this.duplicateIndexes.hasNext()) {
                        nextDuplicateIndex = this.duplicateIndexes.next();
                    } else {
                        // Only happens once we've marked all the duplicates
                        nextDuplicateIndex = -1;
                    }
                } else {
                    rec.setDuplicateReadFlag(false);
                }
            }
            recordInFileIndex++;

            if (!this.REMOVE_DUPLICATES || !rec.getDuplicateReadFlag()) {
                if (PROGRAM_RECORD_ID != null) {
                    rec.setAttribute(SAMTag.PG.name(), chainedPgIds.get(rec.getStringAttribute(SAMTag.PG.name())));
                }
                out.addAlignment(rec);
                progress.record(rec);
            }
        }

        // remember to close the inputs
        iterator.close();

        this.duplicateIndexes.cleanup();

        reportMemoryStats("Before output close");
        out.close();
        reportMemoryStats("After output close");

        // Write out the metrics
        finalizeAndWriteMetrics(libraryIdGenerator);

        return 0;
    }

    /**
     * package-visible for testing
     */
    long numOpticalDuplicates() { return ((long) this.libraryIdGenerator.getOpticalDuplicatesByLibraryIdMap().getSumOfValues()); } // cast as long due to returning a double

    /** Print out some quick JVM memory stats. */
    private void reportMemoryStats(final String stage) {
        System.gc();
        final Runtime runtime = Runtime.getRuntime();
        log.info(stage + " freeMemory: " + runtime.freeMemory() + "; totalMemory: " + runtime.totalMemory() +
                "; maxMemory: " + runtime.maxMemory());
    }

    /**
     * Goes through all the records in a file and generates a set of ReadEndsForMarkDuplicates objects that
     * hold the necessary information (reference sequence, 5' read coordinate) to do
     * duplication, caching to disk as necssary to sort them.
     */
    private void buildSortedReadEndLists(final boolean useBarcodes) {
        final int sizeInBytes;
        if (useBarcodes) {
            sizeInBytes = ReadEndsForMarkDuplicatesWithBarcodes.getSizeOf();
        } else {
            sizeInBytes = ReadEndsForMarkDuplicates.getSizeOf();
        }
        MAX_RECORDS_IN_RAM = (int) (Runtime.getRuntime().maxMemory() / sizeInBytes) / 2;
        final int maxInMemory = (int) ((Runtime.getRuntime().maxMemory() * SORTING_COLLECTION_SIZE_RATIO) / sizeInBytes);
        log.info("Will retain up to " + maxInMemory + " data points before spilling to disk.");

        final ReadEndsForMarkDuplicatesCodec fragCodec, pairCodec, diskCodec;
        if (useBarcodes) {
            fragCodec = new ReadEndsForMarkDuplicatesWithBarcodesCodec();
            pairCodec = new ReadEndsForMarkDuplicatesWithBarcodesCodec();
            diskCodec = new ReadEndsForMarkDuplicatesWithBarcodesCodec();
        } else {
            fragCodec = new ReadEndsForMarkDuplicatesCodec();
            pairCodec = new ReadEndsForMarkDuplicatesCodec();
            diskCodec = new ReadEndsForMarkDuplicatesCodec();
        }

        this.pairSort = SortingCollection.newInstance(ReadEndsForMarkDuplicates.class,
                pairCodec,
                new ReadEndsMDComparator(useBarcodes),
                maxInMemory,
                TMP_DIR);

        this.fragSort = SortingCollection.newInstance(ReadEndsForMarkDuplicates.class,
                fragCodec,
                new ReadEndsMDComparator(useBarcodes),
                maxInMemory,
                TMP_DIR);

        final SamHeaderAndIterator headerAndIterator = openInputs();
        final SAMFileHeader header = headerAndIterator.header;
        final MemoryBasedReadEndsForMarkDuplicatesMap tmp = new MemoryBasedReadEndsForMarkDuplicatesMap(/*MAX_FILE_HANDLES_FOR_READ_ENDS_MAP, diskCodec*/);
        final ProgressLogger progress = new ProgressLogger(log, (int) 1e6, "Read");
        final CloseableIterator<SAMRecord> iterator = headerAndIterator.iterator;

        if (null == this.libraryIdGenerator) {
            this.libraryIdGenerator = new LibraryIdGenerator(header);
        }
        
/*        System.out.println("Obtaining");
        Iterable<SAMRecord> iterable = () -> iterator;
        Stream<SAMRecord> targetStream = StreamSupport.stream(iterable.spliterator(), true);*/
        
        Stream<SAMRecord> targetStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.SORTED),
                true);
      
        System.out.println("Obtained");
        
        final Set<String> keyList = new TreeSet<String>();
//      while (iterator.hasNext()) {}

/*        targetStream.parallel().forEach(rec -> {
        	System.out.println("rec " + rec.getStart() + " Thread " + Thread.currentThread().getName());
        });
*/        
        targetStream.parallel().forEach(rec -> {

//            final SAMRecord rec = iterator.next();

            // This doesn't have anything to do with building sorted ReadEnd lists, but it can be done in the same pass
            // over the input
            if (PROGRAM_RECORD_ID != null) {
                // Gather all PG IDs seen in merged input files in first pass.  These are gathered for two reasons:
                // - to know how many different PG records to create to represent this program invocation.
                // - to know what PG IDs are already used to avoid collisions when creating new ones.
                // Note that if there are one or more records that do not have a PG tag, then a null value
                // will be stored in this set.
                pgIdsSeen.add(rec.getStringAttribute(SAMTag.PG.name()));
            }

            if (rec.getReadUnmappedFlag()) {
                if (rec.getReferenceIndex() == -1) {
                    // When we hit the unmapped reads with no coordinate, no reason to continue.
                    return;
                }
                // If this read is unmapped but sorted with the mapped reads, just skip it.
            } else if (!rec.isSecondaryOrSupplementary()) {
                final ReadEndsForMarkDuplicates fragmentEnd = buildReadEnds(header, index, rec, useBarcodes);
                final String key = rec.getAttribute(ReservedTagConstants.READ_GROUP_ID) + ":" + rec.getReadName();
                if(keyList.contains(key)){
                	return;
                }
                keyList.add(key);
                this.fragSort.add(fragmentEnd);

                if (rec.getReadPairedFlag() && !rec.getMateUnmappedFlag()) {
                    ReadEndsForMarkDuplicates pairedEnds = tmp.remove(rec.getReferenceIndex(), key);

                    // See if we've already seen the first end or not
                    if (pairedEnds == null) {
                    pairedEnds = buildReadEnds(header, index, rec, useBarcodes);
                    tmp.put(pairedEnds.read2ReferenceIndex, key, pairedEnds);

                    	
                    } else {
                        final int sequence = fragmentEnd.read1ReferenceIndex;
                        final int coordinate = fragmentEnd.read1Coordinate;

                        // Set orientationForOpticalDuplicates, which always goes by the first then the second end for the strands.  NB: must do this
                        // before updating the orientation later.
                        if (rec.getFirstOfPairFlag()) {
                            pairedEnds.orientationForOpticalDuplicates = ReadEnds.getOrientationByte(rec.getReadNegativeStrandFlag(), pairedEnds.orientation == ReadEnds.R);
                            if (useBarcodes)
                                ((ReadEndsForMarkDuplicatesWithBarcodes) pairedEnds).readOneBarcode = getReadOneBarcodeValue(rec);
                        } else {
                            pairedEnds.orientationForOpticalDuplicates = ReadEnds.getOrientationByte(pairedEnds.orientation == ReadEnds.R, rec.getReadNegativeStrandFlag());
                            if (useBarcodes)
                                ((ReadEndsForMarkDuplicatesWithBarcodes) pairedEnds).readTwoBarcode = getReadTwoBarcodeValue(rec);
                        }

                        // If the second read is actually later, just add the second read data, else flip the reads
                        if (sequence > pairedEnds.read1ReferenceIndex ||
                                (sequence == pairedEnds.read1ReferenceIndex && coordinate >= pairedEnds.read1Coordinate)) {
                            pairedEnds.read2ReferenceIndex = sequence;
                            pairedEnds.read2Coordinate = coordinate;
                            pairedEnds.read2IndexInFile = index;
                            pairedEnds.orientation = ReadEnds.getOrientationByte(pairedEnds.orientation == ReadEnds.R,
                                    rec.getReadNegativeStrandFlag());
                        } else {
                            pairedEnds.read2ReferenceIndex = pairedEnds.read1ReferenceIndex;
                            pairedEnds.read2Coordinate = pairedEnds.read1Coordinate;
                            pairedEnds.read2IndexInFile = pairedEnds.read1IndexInFile;
                            pairedEnds.read1ReferenceIndex = sequence;
                            pairedEnds.read1Coordinate = coordinate;
                            pairedEnds.read1IndexInFile = index;
                            pairedEnds.orientation = ReadEnds.getOrientationByte(rec.getReadNegativeStrandFlag(),
                                    pairedEnds.orientation == ReadEnds.R);
                        }

                        pairedEnds.score += DuplicateScoringStrategy.computeDuplicateScore(rec, this.DUPLICATE_SCORING_STRATEGY);
                        this.pairSort.add(pairedEnds);
                    }
                }
            }

            // Print out some stats every 1m reads
            ++index;
            if (progress.record(rec)) {
                log.info("Tracking " + tmp.size() + " as yet unmatched pairs. " + tmp.sizeInRam() + " records in RAM.");
            }
        
        });
        

        log.info("Read " + index + " records. " + tmp.size() + " pairs never matched.");
        iterator.close();

        // Tell these collections to free up memory if possible.
        this.pairSort.doneAdding();
        this.fragSort.doneAdding();
    }

    /** Builds a read ends object that represents a single read. */
    private ReadEndsForMarkDuplicates buildReadEnds(final SAMFileHeader header, final long index, final SAMRecord rec, final boolean useBarcodes) {
        final ReadEndsForMarkDuplicates ends;

        if (useBarcodes) {
            ends = new ReadEndsForMarkDuplicatesWithBarcodes();
        } else {
            ends = new ReadEndsForMarkDuplicates();
        }
        ends.read1ReferenceIndex = rec.getReferenceIndex();
        ends.read1Coordinate = rec.getReadNegativeStrandFlag() ? rec.getUnclippedEnd() : rec.getUnclippedStart();
        ends.orientation = rec.getReadNegativeStrandFlag() ? ReadEnds.R : ReadEnds.F;
        ends.read1IndexInFile = index;
        ends.score = DuplicateScoringStrategy.computeDuplicateScore(rec, this.DUPLICATE_SCORING_STRATEGY);

        // Doing this lets the ends object know that it's part of a pair
        if (rec.getReadPairedFlag() && !rec.getMateUnmappedFlag()) {
            ends.read2ReferenceIndex = rec.getMateReferenceIndex();
        }

        // Fill in the library ID
        ends.libraryId = libraryIdGenerator.getLibraryId(rec);

        // Fill in the location information for optical duplicates
        if (this.opticalDuplicateFinder.addLocationInformation(rec.getReadName(), ends)) {
            // calculate the RG number (nth in list)
            ends.readGroup = 0;
            final String rg = (String) rec.getAttribute("RG");
            final List<SAMReadGroupRecord> readGroups = header.getReadGroups();

            if (rg != null && readGroups != null) {
                for (final SAMReadGroupRecord readGroup : readGroups) {
                    if (readGroup.getReadGroupId().equals(rg)) break;
                    else ends.readGroup++;
                }
            }
        }

        if (useBarcodes) {
            final ReadEndsForMarkDuplicatesWithBarcodes endsWithBarcode = (ReadEndsForMarkDuplicatesWithBarcodes) ends;
            endsWithBarcode.barcode = getBarcodeValue(rec);
            if (rec.getFirstOfPairFlag()) {
                endsWithBarcode.readOneBarcode = getReadOneBarcodeValue(rec);
            } else {
                endsWithBarcode.readTwoBarcode = getReadTwoBarcodeValue(rec);
            }
        }

        return ends;
    }

    /**
     * Goes through the accumulated ReadEndsForMarkDuplicates objects and determines which of them are
     * to be marked as duplicates.
     *
     * @return an array with an ordered list of indexes into the source file
     */
    private void generateDuplicateIndexes(final boolean useBarcodes) {
        // Keep this number from getting too large even if there is a huge heap.
        final int maxInMemory = (int) Math.min((Runtime.getRuntime().maxMemory() * 0.25) / SortingLongCollection.SIZEOF,
                (double) (Integer.MAX_VALUE - 5));
        log.info("Will retain up to " + maxInMemory + " duplicate indices before spilling to disk.");
        this.duplicateIndexes = new SortingLongCollection(maxInMemory, TMP_DIR.toArray(new File[TMP_DIR.size()]));

        ReadEndsForMarkDuplicates firstOfNextChunk = null;
        final List<ReadEndsForMarkDuplicates> nextChunk = new ArrayList<ReadEndsForMarkDuplicates>(200);

        // First just do the pairs
        log.info("Traversing read pair information and detecting duplicates.");
        for (final ReadEndsForMarkDuplicates next : this.pairSort) {
            if (firstOfNextChunk == null) {
                firstOfNextChunk = next;
                nextChunk.add(firstOfNextChunk);
            } else if (areComparableForDuplicates(firstOfNextChunk, next, true, useBarcodes)) {
                nextChunk.add(next);
            } else {
                if (nextChunk.size() > 1) {
                    markDuplicatePairs(nextChunk);
                }

                nextChunk.clear();
                nextChunk.add(next);
                firstOfNextChunk = next;
            }
        }
        if (nextChunk.size() > 1) markDuplicatePairs(nextChunk);
        this.pairSort.cleanup();
        this.pairSort = null;

        // Now deal with the fragments
        log.info("Traversing fragment information and detecting duplicates.");
        boolean containsPairs = false;
        boolean containsFrags = false;

        for (final ReadEndsForMarkDuplicates next : this.fragSort) {
            if (firstOfNextChunk != null && areComparableForDuplicates(firstOfNextChunk, next, false, useBarcodes)) {
                nextChunk.add(next);
                containsPairs = containsPairs || next.isPaired();
                containsFrags = containsFrags || !next.isPaired();
            } else {
                if (nextChunk.size() > 1 && containsFrags) {
                    markDuplicateFragments(nextChunk, containsPairs);
                }

                nextChunk.clear();
                nextChunk.add(next);
                firstOfNextChunk = next;
                containsPairs = next.isPaired();
                containsFrags = !next.isPaired();
            }
        }
        markDuplicateFragments(nextChunk, containsPairs);
        this.fragSort.cleanup();
        this.fragSort = null;

        log.info("Sorting list of duplicate records.");
        this.duplicateIndexes.doneAddingStartIteration();
    }

    private boolean areComparableForDuplicates(final ReadEndsForMarkDuplicates lhs, final ReadEndsForMarkDuplicates rhs, final boolean compareRead2, final boolean useBarcodes) {
        boolean areComparable = lhs.libraryId == rhs.libraryId;

        if (useBarcodes && areComparable) { // areComparable is useful here to avoid the casts below
            final ReadEndsForMarkDuplicatesWithBarcodes lhsWithBarcodes = (ReadEndsForMarkDuplicatesWithBarcodes) lhs;
            final ReadEndsForMarkDuplicatesWithBarcodes rhsWithBarcodes = (ReadEndsForMarkDuplicatesWithBarcodes) rhs;
            areComparable = lhsWithBarcodes.barcode == rhsWithBarcodes.barcode &&
                    lhsWithBarcodes.readOneBarcode == rhsWithBarcodes.readOneBarcode &&
                    lhsWithBarcodes.readTwoBarcode == rhsWithBarcodes.readTwoBarcode;
        }

        if (areComparable) {
            areComparable = lhs.read1ReferenceIndex == rhs.read1ReferenceIndex &&
                    lhs.read1Coordinate == rhs.read1Coordinate &&
                    lhs.orientation == rhs.orientation;
        }

        if (areComparable && compareRead2) {
            areComparable = lhs.read2ReferenceIndex == rhs.read2ReferenceIndex &&
                    lhs.read2Coordinate == rhs.read2Coordinate;
        }

        return areComparable;
    }

    private void addIndexAsDuplicate(final long bamIndex) {
        this.duplicateIndexes.add(bamIndex);
        ++this.numDuplicateIndices;
    }

    /**
     * Takes a list of ReadEndsForMarkDuplicates objects and removes from it all objects that should
     * not be marked as duplicates.  This assumes that the list contains objects representing pairs.
     *
     * @param list
     */
    private void markDuplicatePairs(final List<ReadEndsForMarkDuplicates> list) {
        short maxScore = 0;
        ReadEndsForMarkDuplicates best = null;

        /** All read ends should have orientation FF, FR, RF, or RR **/
        for (final ReadEndsForMarkDuplicates end : list) {
            if (end.score > maxScore || best == null) {
                maxScore = end.score;
                best = end;
            }
        }

        for (final ReadEndsForMarkDuplicates end : list) {
            if (end != best) {
                addIndexAsDuplicate(end.read1IndexInFile);
                addIndexAsDuplicate(end.read2IndexInFile);
            }
        }

        if (this.READ_NAME_REGEX != null) {
            AbstractMarkDuplicatesCommandLineProgram.trackOpticalDuplicates(list, opticalDuplicateFinder, libraryIdGenerator);
        }
    }

    /**
     * Takes a list of ReadEndsForMarkDuplicates objects and removes from it all objects that should
     * not be marked as duplicates.  This will set the duplicate index for only list items are fragments.
     *
     * @param list
     * @param containsPairs true if the list also contains objects containing pairs, false otherwise.
     */
    private void markDuplicateFragments(final List<ReadEndsForMarkDuplicates> list, final boolean containsPairs) {
        if (containsPairs) {
            for (final ReadEndsForMarkDuplicates end : list) {
                if (!end.isPaired()) addIndexAsDuplicate(end.read1IndexInFile);
            }
        } else {
            short maxScore = 0;
            ReadEndsForMarkDuplicates best = null;
            for (final ReadEndsForMarkDuplicates end : list) {
                if (end.score > maxScore || best == null) {
                    maxScore = end.score;
                    best = end;
                }
            }

            for (final ReadEndsForMarkDuplicates end : list) {
                if (end != best) {
                    addIndexAsDuplicate(end.read1IndexInFile);
                }
            }
        }
    }

    // To avoid overflows or underflows when subtracting two large (positive and negative) numbers
    static int compareInteger(final int x, final int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    /** Comparator for ReadEndsForMarkDuplicates that orders by read1 position then pair orientation then read2 position. */
    static class ReadEndsMDComparator implements Comparator<ReadEndsForMarkDuplicates> {

        final boolean useBarcodes;

        public ReadEndsMDComparator(final boolean useBarcodes) {
            this.useBarcodes = useBarcodes;
        }

        public int compare(final ReadEndsForMarkDuplicates lhs, final ReadEndsForMarkDuplicates rhs) {
            int compareDifference = lhs.libraryId - rhs.libraryId;
            if (useBarcodes) {
                final ReadEndsForMarkDuplicatesWithBarcodes lhsWithBarcodes = (ReadEndsForMarkDuplicatesWithBarcodes) lhs;
                final ReadEndsForMarkDuplicatesWithBarcodes rhsWithBarcodes = (ReadEndsForMarkDuplicatesWithBarcodes) rhs;
                if (compareDifference == 0) compareDifference = compareInteger(lhsWithBarcodes.barcode, rhsWithBarcodes.barcode);
                if (compareDifference == 0) compareDifference = compareInteger(lhsWithBarcodes.readOneBarcode, rhsWithBarcodes.readOneBarcode);
                if (compareDifference == 0) compareDifference = compareInteger(lhsWithBarcodes.readTwoBarcode, rhsWithBarcodes.readTwoBarcode);
            }
            if (compareDifference == 0) compareDifference = lhs.read1ReferenceIndex - rhs.read1ReferenceIndex;
            if (compareDifference == 0) compareDifference = lhs.read1Coordinate - rhs.read1Coordinate;
            if (compareDifference == 0) compareDifference = lhs.orientation - rhs.orientation;
            if (compareDifference == 0) compareDifference = lhs.read2ReferenceIndex - rhs.read2ReferenceIndex;
            if (compareDifference == 0) compareDifference = lhs.read2Coordinate - rhs.read2Coordinate;
            if (compareDifference == 0) compareDifference = (int) (lhs.read1IndexInFile - rhs.read1IndexInFile);
            if (compareDifference == 0) compareDifference = (int) (lhs.read2IndexInFile - rhs.read2IndexInFile);

            return compareDifference;
        }
    }
    
    class MemoryBasedReadEndsForMarkDuplicatesMap implements ReadEndsForMarkDuplicatesMap {

        /**
         * Index of this list is sequence index.  Value is map from String {read group id:read name} to ReadEnds.
         * When a ReadEnds is put into this container, it is stored according to the sequenceIndex of the mate
         */
        private final List<Map<String, ReadEndsForMarkDuplicates>> mapPerSequence = new ArrayList<Map<String, ReadEndsForMarkDuplicates>>();

        public ReadEndsForMarkDuplicates remove(int mateSequenceIndex, String key) {
            if (mateSequenceIndex >= mapPerSequence.size()) {
                return null;
            }
            return mapPerSequence.get(mateSequenceIndex).remove(key);
        }

        public void put(int mateSequenceIndex, String key, ReadEndsForMarkDuplicates readEnds) {
            while (mateSequenceIndex >= mapPerSequence.size()) {
                mapPerSequence.add(new HashMap<String, ReadEndsForMarkDuplicates>());
            }
            mapPerSequence.get(mateSequenceIndex).put(key, readEnds);
        }

        public int size() {
            int total = 0;
            for (Map<String, ReadEndsForMarkDuplicates> map : mapPerSequence) {
                total += map.size();
            }
            return total;
        }

        /**
         * @return number of elements stored in RAM.  Always <= size()
         */
        public int sizeInRam() {
            return size();
        }
    }

}
