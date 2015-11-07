package com.workflow.application.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util {

	public static List<String> getBWACommand(String groupID){
		List<String> command = new ArrayList<String>();
    	command.add("/cac/u01/jz362/Workflow/bwa/bwa-0.7.12/bwa");
    	command.add("mem");
    	command.add("-M");
    	command.add("-t");
    	command.add("20");
    	command.add("-R");
    	command.add("\"@RG\\tID:group"+groupID+"\\tSM:SRR622457\\tPL:illumina\\tLB:lib1\\tPU:unit1\"");
    	command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
    	return command;
	}
	
	public static List<String> getPipeSortCommand(){
		List<String> command = new ArrayList<String>();
		command.add("|");
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
		command.add("sort");
		command.add("-");
		return command;
	}
	
	public static List<String> getContigsListCommand(String directoryName){
		//~/Workflow/samtools/samtools-1.2/samtools view -H 542.0115432785603_clusterTest.bam | grep "^@SQ" | awk -F":" '{print $2}' | awk '{print $1}' > contigs.txt
		List<String> command = new ArrayList<String>();
		command.add("mkdir "+directoryName+";");
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
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
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
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
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
		command.add("index");
		return command;
	}

	public static void writeShAndStartProcess(List<String> commands,String workingDir, double random,String nameExtension){
		StringBuilder builder = new StringBuilder();
		for(String command:commands){
			builder.append(command+" ");
		}
		String shFile = random+nameExtension;
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(workingDir,shFile)))){
			writer.write("#!/bin/sh");
			writer.write("\n");
			writer.write(builder.toString());
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		builder = null;
		
		runProcessWithShell(workingDir, shFile);
	}
	
	public static List<String> getRealignerTargetCreatorCommand(int numberOfThreads,String inputBam,String outputFile){
		List<String> command = new ArrayList<>();
		
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T RealignerTargetCreator -nt $NUMDATATHREADS -R 
		//$REFERENCEDIR -I $REORDERBAM -o $OUTPUTINTERVALS --defaultBaseQualities 1

		command.add("-T");
		command.add("RealignerTargetCreator");
		command.add("-nt");
		command.add(numberOfThreads+"");
		command.add("-R");
		command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
		command.add("-I");
		command.add(inputBam);
		command.add("-defaultBaseQualities");
		command.add("1");
		command.add("-o");
		command.add(outputFile);
		
		return command;
	}
	
	public static List<String> getIndelRealignerCommand(String inputBam,String inputIntervals,String outputBam){
		List<String> command= new ArrayList<>();
		
		//java -Djava.io.tmpdir=$TEMPDIR -jar $GATKJARDIR/GenomeAnalysisTK.jar -T IndelRealigner -R $REFERENCEDIR -I $REORDERBAM 
		//-targetIntervals $OUTPUTINTERVALS -o $REALIGNEDBAM -LOD 5 --defaultBaseQualities 1

		command.add("-T");
		command.add("IndelRealigner");
		command.add("-R");
		command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
		command.add("-I");
		command.add(inputBam);
		command.add("-defaultBaseQualities");
		command.add("1");
		command.add("-o");
		command.add(outputBam);
		
		return command;
	}
	

	public static List<String> getBAMMergeCommand(String outputBAM){
		List<String> command = new ArrayList<String>();
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
		command.add("merge");
		command.add(outputBAM);
		return command;
	}
	
	public static List<String> getBaseRecalibratorCommand(int numberOfThreads,String inputBam,String outputFile){
		List<String> command = new ArrayList<String>();
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T BaseRecalibrator -nct $NUMCPUTHREADS -R $REFERENCEDIR 
		//-I $REALIGNEDBAM -o $BASECALIBRATEDCSV -cov ReadGroupCovariate -cov QualityScoreCovariate -cov CycleCovariate -cov 
		//ContextCovariate -knownSites $DBSNFP135VCF
		command.add("-T");
		command.add("BaseRecalibrator");
		command.add("-nct");
		command.add(numberOfThreads+"");
		command.add("-R");
		command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
		command.add("-I");
		command.add(inputBam);
		command.add("-o");
		command.add(outputFile);
		command.add("-cov");
		command.add("ReadGroupCovariate");
		command.add("-cov");
		command.add("CycleCovariate");
		command.add("-cov");
		command.add("ContextCovariate");
		command.add("-knownSites");
		command.add("/cac/u01/jz362/Workflow/dbsnp/dbsnp_137.hg19.vcf");
		
		return command;
	}
	
	public static List<String> getPrintReadsCommand(int numberOfThreads,String inputBam,String outputBAM,String calibratedCSV){
		//java -jar $GATKJARDIR/GenomeAnalysisTK.jar -T PrintReads -nct $NUMCPUTHREADS -baq 
		//RECALCULATE -baqGOP 30 -R $REFERENCEDIR -I $REALIGNEDBAM -BQSR $BASECALIBRATEDCSV -o $RECALIBRATEDBAM
		List<String> command = new ArrayList<String>();
		
		command.add("-T");
		command.add("PrintReads");
		command.add("-nct");
		command.add(numberOfThreads+"");
		command.add("-R");
		command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
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
		
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
		command.add("view");
		command.add("-b");
		command.add(inputBAM);
		command.add(chr);
		command.add(">");
		command.add(outputBAM);
		
		
		return command;
	}
	
	public static void runProcessWithShell(String workingDir,String shFile){
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList("bash",workingDir+File.separator+shFile));
		
		runProcess(pb);
	}
	
	public static void runProcessWithListOfCommands(List<String> command){
		ProcessBuilder pb = new ProcessBuilder(command);
		runProcess(pb);
	}

	private static void runProcess(ProcessBuilder pb) {
		try {
			Process process = pb.start();
			
			int exitValue = process.waitFor();
			final InputStream errorStream = process.getErrorStream();
			new Runnable() {
				
				@Override
				public void run() {
					extracted(errorStream);
				}
			}.run();
			final InputStream inputStream = process.getInputStream();
			new Runnable(){

				@Override
				public void run() {
					
					extracted(inputStream);
				}
				
			}.run();
			Logger.getLogger(Util.class.getName()).log(Level.INFO,"Exit Value: "+ exitValue);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	
}
