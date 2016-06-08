package com.workflow.application.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.common.collect.Lists;
import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.tasks.worker.ContigSplitBAMWorker;
import com.workflow.application.tasks.worker.PoolFactory;
import com.workflow.application.tasks.worker.Worker;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import tassl.application.cometcloud.FileProperties;

/**
 * @author ajay
 *
 */
public class Util {

	
	public static Set<FileProperties> NtoNTasks = new HashSet<>();
	public static String BWAExecutable;
	public static String SAMTOOLSExecutable;
	public static String GATKJar;
	public static String ReferenceFile;
	public static String DBSNPFile;
	
	static {
		/*BWAExecutable = "/util/academic/bwa/bwa-0.7.12/bwa";
		SAMTOOLSExecutable = "/util/academic/samtools/samtools-1.1/samtools";
		GATKJar = "/util/academic/gatk/gatk-protected/target/GenomeAnalysisTK.jar";
		ReferenceFile = "/projects/academic/jzola/ajaysudh/data/Reference/hg19.fasta";
		DBSNPFile = "/projects/academic/jzola/ajaysudh/data/dbsnp/dbsnp_137.hg19.vcf";*/
		BWAExecutable = System.getenv("bwaExecutable")!=null?System.getenv("bwaExecutable"):"/util/academic/bwa/bwa-0.7.12/bwa";
		SAMTOOLSExecutable = System.getenv("samtoolsExecutable")!=null?System.getenv("samtoolsExecutable"):"/util/academic/samtools/samtools-1.1/samtools";
		GATKJar = System.getenv("gatkJar")!=null?System.getenv("gatkJar"):"/util/academic/gatk/gatk-protected/target/GenomeAnalysisTK.jar";
		ReferenceFile = System.getenv("referenceFastqFile")!=null?System.getenv("referenceFastqFile"):"/projects/academic/jzola/ajaysudh/data/Reference/hg19.fasta";
		DBSNPFile = System.getenv("dbsnpFile")!=null?System.getenv("dbsnpFile"):"/projects/academic/jzola/ajaysudh/data/dbsnp/dbsnp_137.hg19.vcf";
		HelperConstants.numberOfThreads = Runtime.getRuntime().availableProcessors();
	}
	
	public static void initFilePaths(String bwa, String samtools, String gatk, String reference, String dbsnp){
		/*BWAExecutable = bwa==null?System.getenv("bwaExecutable"):bwa;
		SAMTOOLSExecutable = samtools==null?System.getenv("samtoolsExecutable"):samtools;
		GATKJar = gatk==null?System.getenv("gatkJar"):gatk;
		ReferenceFile = reference==null?System.getenv("referenceFastqFile"):reference;
		DBSNPFile = dbsnp==null?System.getenv("dbsnpFile"):dbsnp;
*/
				
		HelperConstants.numberOfThreads = Runtime.getRuntime().availableProcessors();
	}
	
	public static List<String> getBWACommand(String groupID){
		List<String> command = new ArrayList<String>();
    	command.add(BWAExecutable);
    	command.add("mem");
    	command.add("-M");
    	command.add("-t");
    	command.add(HelperConstants.numberOfThreads+"");
    	command.add("-R");
    	command.add("\"@RG\\tID:group"+groupID+"\\tSM:SRR622457\\tPL:illumina\\tLB:lib1\\tPU:unit1\"");
    	command.add(ReferenceFile);
    	return command;
	}
	
	public static List<String> getPipeSortCommand(){
		List<String> command = new ArrayList<String>();
		command.add("|");
		command.add(SAMTOOLSExecutable);
		command.add("sort");
		command.add("-@");
		command.add(HelperConstants.numberOfThreads+"");
		command.add("-");
		return command;
	}
	
	public static List<String> getContigsListCommand(String inputBam){
		//~/Workflow/samtools/samtools-1.2/samtools view -H 542.0115432785603_clusterTest.bam | grep "^@SQ" | awk -F":" '{print $2}' | awk '{print $1}' > contigs.txt
		List<String> command = new ArrayList<String>();
		command.add(SAMTOOLSExecutable);
		command.add("view");
		command.add("-H");
		command.add(inputBam);
		command.add("|");
		command.add("grep \"^@SQ\"");
		command.add("|");
		command.add("awk -F\":\" '{print $2}'");
		command.add("|");
		command.add("awk '{print $1}'");
		command.add(">");
		return command;
	}
	
	public static List<String> getSplitBamByContigs(String contigsFile,String bamFile,String outputDirectory){
		//for c in `cat contigs.txt` ; do echo processing $c; ~/Workflow/samtools/samtools-1.2/samtools view -bh 542.0115432785603_clusterTest.bam $c > split/$c.bam; don
		List<String> command = new ArrayList<String>();
		command.add("for c in `cat "+contigsFile+"`;");
		command.add(SAMTOOLSExecutable);
		command.add("view");
		command.add("-bh");
		command.add(bamFile);
		command.add("$c");
		command.add(">");
		command.add(outputDirectory+"/$c.bam");
		command.add(";");
		command.add("done");
		return command;
	}
	
	public static List<String> getIndexCommand(){
		List<String> command = new ArrayList<String>();
		command.add(SAMTOOLSExecutable);
		command.add("index");
		return command;
	}

	public static boolean writeShAndStartProcess(List<String> commands,String workingDir, double random,String nameExtension){
		StringBuilder builder = new StringBuilder();
		for(String command:commands){
			builder.append(command+" ");
		}
		String shFile = random+nameExtension;
		File shellFile = new File(workingDir,shFile);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(shellFile))){
			writer.write("#!/bin/sh");
			writer.write("\n");
			writer.write(builder.toString());
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		builder = null;
		
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        
        try {
			Files.setPosixFilePermissions(shellFile.toPath(), perms);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		return runProcessWithShell(workingDir, shFile);
	}
	
	public static List<String> getRealignerTargetCreatorCommand(String inputBam,String outputFile){
		List<String> command = new ArrayList<>();
		
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T RealignerTargetCreator -nt $NUMDATATHREADS -R 
		//$REFERENCEDIR -I $REORDERBAM -o $OUTPUTINTERVALS --defaultBaseQualities 1

		command.add("java");
		command.add("-jar");
		command.add(GATKJar);
		
		command.add("-T");
		command.add("RealignerTargetCreator");
		command.add("-nt");
		command.add(HelperConstants.numberOfThreads+"");
		command.add("-R");
		command.add(ReferenceFile);
		command.add("-I");
		command.add(inputBam);
		command.add("--defaultBaseQualities");
		command.add("1");
		command.add("-o");
		command.add(outputFile);
		
		return command;
	}
	
	public static List<String> getIndelRealignerCommand(String inputBam,String inputIntervals,String outputBam){
		List<String> command= new ArrayList<>();
		
		//java -Djava.io.tmpdir=$TEMPDIR -jar $GATKJARDIR/GenomeAnalysisTK.jar -T IndelRealigner -R $REFERENCEDIR -I $REORDERBAM 
		//-targetIntervals $OUTPUTINTERVALS -o $REALIGNEDBAM -LOD 5 --defaultBaseQualities 1

		command.add("java");
		command.add("-jar");
		command.add(GATKJar);
		
		
		command.add("-T");
		command.add("IndelRealigner");
		command.add("-R");
		command.add(ReferenceFile);
		command.add("-I");
		command.add(inputBam);
		command.add("-targetIntervals");
		command.add(inputIntervals);
		command.add("--defaultBaseQualities");
		command.add("1");
		command.add("-o");
		command.add(outputBam);
		
		return command;
	}
	

	public static List<String> getBAMMergeCommand(String outputBAM){
		List<String> command = new ArrayList<String>();
		command.add(SAMTOOLSExecutable);
		command.add("merge");
		command.add(outputBAM);
		return command;
	}
	
	public static List<String> getBaseRecalibratorCommand(List<String> inputBam,String outputFile){
		List<String> command = new ArrayList<String>();
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T BaseRecalibrator -nct $NUMCPUTHREADS -R $REFERENCEDIR 
		//-I $REALIGNEDBAM -o $BASECALIBRATEDCSV -cov ReadGroupCovariate -cov QualityScoreCovariate -cov CycleCovariate -cov 
		//ContextCovariate -knownSites $DBSNFP135VCF
		
		command.add("java");
		command.add("-jar");
		command.add(GATKJar);
		
		
		command.add("-T");
		command.add("BaseRecalibrator");
		command.add("-nct");
		command.add(HelperConstants.numberOfThreads+"");
		command.add("-R");
		command.add(ReferenceFile);
//		command.add("-I");
		command.addAll(inputBam);
		command.add("-o");
		command.add(outputFile);
		command.add("-cov");
		command.add("ReadGroupCovariate");
		command.add("-cov");
		command.add("CycleCovariate");
		command.add("-cov");
		command.add("ContextCovariate");
		command.add("-knownSites");
		command.add(DBSNPFile);
		
		return command;
	}
	
	public static List<String> getPrintReadsCommand(String inputBam,String outputBAM,String calibratedCSV){
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T PrintReads -nct $NUMCPUTHREADS -baq 
		//RECALCULATE -baqGOP 30 -R $REFERENCEDIR -I $REALIGNEDBAM -BQSR $BASECALIBRATEDCSV -o $RECALIBRATEDBAM
		List<String> command = new ArrayList<String>();
		
		command.add("java");
		command.add("-jar");
		command.add(GATKJar);
		
		
		command.add("-T");
		command.add("PrintReads");
		command.add("-nct");
		command.add(HelperConstants.numberOfThreads+"");
		command.add("-R");
		command.add(ReferenceFile);
		command.add("-I");
		command.add(inputBam);
		command.add("-o");
		command.add(outputBAM);
		command.add("-baq");
		command.add("RECALCULATE");
		command.add("-baqGOP");
		command.add("30");
		command.add("-BQSR");
		command.add(calibratedCSV);
		
		return command;
	}
	
	public static List<String> getSplitBAMByChromosomeCommand(String chr,String inputBAM,String outputBAM){
		List<String> command = new ArrayList<String>();

		//samtools view -b in.bam chr1 > in_chr1.bam
		//samtools view -b -f 4 in.bam > unmapped.bam

		command.add(SAMTOOLSExecutable);
		command.add("view");
		command.add("-bo");
		command.add(outputBAM);
		command.add(inputBAM);
		if(!chr.equalsIgnoreCase("unmapped"))
			command.add(chr);
		else{
			command.add("-f");
			command.add("4");
		}
		
		return command;
	}
	
	public static List<String> getHaplotypeCallerCommand(String inputBam,String outputVCF, String contig, String bqsrFile){
		//-T HaplotypeCaller -nct $NUMCPUTHREADS -R $REFERENCEDIR -I $RECALIBRATEDBAM -o $OUTPUTVCF -D $DBSNFP135VCF 
		//-A AlleleBalance -A Coverage -A HomopolymerRun -A FisherStrand -A HaplotypeScore -A HardyWeinberg -A ReadPosRankSumTest 
		//-A QualByDepth -A MappingQualityRankSumTest -A VariantType -A MappingQualityZero -minPruning 10 -stand_call_conf 30.0 -stand_emit_conf 10.0
		List<String> command = new ArrayList<String>();

		command.add("java");
		command.add("-jar");
		command.add(GATKJar);
		
		command.add("-T");
		command.add("HaplotypeCaller");
		command.add("-nct");
		command.add(HelperConstants.numberOfThreads+"");
		command.add("-R");
		command.add(ReferenceFile);
		command.add("-I");
		command.add(inputBam);
		command.add("-o");
		command.add(outputVCF);
		command.add("-D");
		command.add(DBSNPFile);
		command.add("-A");
		command.add("AlleleBalance");
		command.add("-A");
		command.add("Coverage");
		command.add("-A");
		command.add("HomopolymerRun");
		command.add("-A");
		command.add("FisherStrand");
		command.add("-A");
		command.add("HaplotypeScore");
		command.add("-A");
		command.add("HardyWeinberg");
		command.add("-A");
		command.add("ReadPosRankSumTest");
		command.add("-A");
		command.add("QualByDepth");
		command.add("-A");
		command.add("MappingQualityRankSumTest");
		command.add("-A");
		command.add("VariantType");
		command.add("-minPruning");
		command.add("10");
		command.add("-stand_call_conf");
		command.add("30.0");
		command.add("-stand_emit_conf");
		command.add("10.0");
		command.add("-L");
		command.add(contig);
		command.add("-BQSR");
		command.add(bqsrFile);
		
		return command;
	}
	
	public static boolean runProcessWithShell(String workingDir,String shFile){
//		ProcessBuilder pb = new ProcessBuilder(Arrays.asList("bash",workingDir+File.separator+shFile));
//		runProcess(pb);
//		runWithRuntimeExec(workingDir+File.separator+shFile);
		return runScript(workingDir+File.separator+shFile);
//		return startNewProcessAndRunScript(workingDir, shFile);
	}
	
	public static boolean startNewProcessAndRunScript(String workingDir,String shFile){
		return runScript("java -Xms512m -Xmx32G -cp " + System.getProperty("java.class.path") + " com.workflow.application.util.ScriptRunner " + workingDir + " " + shFile);
	}
	
	public static boolean runScript(String command){
		System.out.println("Command for Executor: " + command);
		Thread runningThread = createRunningTaskThread();
		runningThread.start();
        CommandLine cmdLine = CommandLine.parse(command);
        
        DefaultExecutor defaultExecutor = new DefaultExecutor();
        defaultExecutor.setExitValue(0);

        ExecuteWatchdog watchdog = new ExecuteWatchdog(180000);
        defaultExecutor.setWatchdog(watchdog);
        
        PumpStreamHandler streamHandler = new PumpStreamHandler(System.out);
        defaultExecutor.setStreamHandler(streamHandler);
        int exitValue=-1;
		try {
            exitValue = defaultExecutor.execute(cmdLine);
            
        } catch (ExecuteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!(defaultExecutor.isFailure(exitValue) && watchdog.killedProcess())) {
        	System.out.println("Exit Value: " + exitValue);
        	runningThread.interrupt();
        	return true;
        }
        return false;
    }

	
	public static void runProcessWithListOfCommands(List<String> command){
		ProcessBuilder pb = new ProcessBuilder(command);
		Logger.getLogger(Util.class.getName()).log(Level.INFO,"command: " + command);

		runProcess(pb);
	}

	private static void runProcess(ProcessBuilder pb) {
		Process process=null;
		
		Thread runnable = createRunningTaskThread();
		runnable.start();
		try {
			pb.redirectErrorStream(true);
			process = pb.start();
			/*final InputStream errorStream = process.getErrorStream();
			new Runnable() {
				
				@Override
				public void run() {
					extracted(errorStream);
				}
			}.run();*/
			/*final InputStream inputStream = process.getInputStream();
			extracted(inputStream);*/
            try (InputStream is = process.getInputStream()) {
                int c;
                while ((c = is.read()) != -1) {
                	System.out.print((char)c);
                }
            }
            int exitValue = process.waitFor();
            runnable.interrupt();
            Logger.getLogger(Util.class.getName()).log(Level.INFO,"Exit Value: "+ exitValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static Thread createRunningTaskThread() {
		Thread runnable = new Thread(new Runnable(){

			@Override
			public void run() {
				while(!Thread.currentThread().isInterrupted()){
					System.out.println("Running the task...");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		return runnable;
	}
	

	private static void extracted(InputStream inputStream) {
		try {
			BufferedReader reader =new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ( (line = reader.readLine()) != null) {
				Logger.getLogger(Util.class.getName()).log(Level.INFO,"Stream "+ line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static List<String> splitBAMbyChromosome(File contigsFile, String inputBAM,List<String> outputBAMs){
		
		List<String> contigsList = new ArrayList<>(24);
		String line;
		try(BufferedReader reader = new BufferedReader(new FileReader(contigsFile))){
			while((line=reader.readLine())!=null){
				contigsList.add(line);
			}
			contigsList.add("unmapped");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<Worker<String>>  bamsplitWorker = new ArrayList<>();
		PoolFactory<String> factory = new PoolFactory<>(bamsplitWorker);
		for (int i = 0; i < contigsList.size(); i++) {
			String contig = contigsList.get(i);
			//			bamsplitWorker.add(new ContigSplitBAMWorker(contig, inputBAM));
			bamsplitWorker.add(new ContigSplitBAMWorker(contig, inputBAM,outputBAMs.get(i)));
		}
		try {
			List<String> runAndGetResults = factory.runAndGetResults();
			return runAndGetResults;
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getStagingLocation(String stagingLocation, String workingDir, String inputFile) {
		if(new File(inputFile).exists())
			return inputFile;
		if(new File(stagingLocation + File.separator+ inputFile).exists())
			return stagingLocation + File.separator+ inputFile;
		if(new File(workingDir + File.separator+ inputFile).exists())
			return workingDir + File.separator+ inputFile;
		return null;
	}
	
	public static List<FileProperties> uploadAndGetResults(InputHelper helper, WorkerTask task, String workingDir,List<String> toUpload, int size) {
		List<FileProperties> resultFiles;
		List<List<String>> partition = Lists.partition(toUpload, size);
		resultFiles = new ArrayList<>();
		for(List<String> list:partition){
			resultFiles.addAll(task.uploadResults(list,workingDir, helper.getOutputFiles().get(0)));
		}
		return resultFiles;
	}
	
	public static String getContigForFile(String bamFile){
		String contig = "chr22";
		
		SamReader reader = SamReaderFactory.makeDefault().open(new File(bamFile));
		
		SAMRecordIterator iterator = reader.iterator();
		while(iterator.hasNext()){
			SAMRecord record = iterator.next();
			if(record.getContig()!=null)
				return record.getContig();
		}
		
		return contig;
	}
	
	public static long getFileSize(String path) {
		File file = new File(path);
		return file.length();
	}
	
	public static void indexBAM(String inputBam){
		List<String> indexCommand = Util.getIndexCommand();
		indexCommand.add(inputBam);
		Util.runProcessWithListOfCommands(indexCommand);
	}
	
	
	/**
	 * @param FQN of inputBAM
	 * @return
	 */
	public static boolean ifIndexExistsForBAM(String inputBAM){
		File input = new File(inputBAM);
		if(new File(input.getParent()+input.getName()+".bai").exists() || new File(input.getParent()+input.getName().replaceAll(".bam", ".bai")).exists())
			return true;
		return false;
		
	}
	
}