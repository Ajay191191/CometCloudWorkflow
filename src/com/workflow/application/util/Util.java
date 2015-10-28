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
	
	public static List<String> getSplitBamByContigs(String contigsFile,String bamFile,String outputFilePattern){
		//for c in `cat contigs.txt` ; do echo processing $c; ~/Workflow/samtools/samtools-1.2/samtools view -bh 542.0115432785603_clusterTest.bam $c > split/$c.bam; don
		List<String> command = new ArrayList<String>();
		command.add("for c in `cat "+contigsFile+"`;");
		command.add("/cac/u01/jz362/Workflow/samtools/samtools-1.2/samtools");
		command.add("view");
		command.add("-bh");
		command.add(bamFile);
		command.add("$c");
		command.add(">");
		command.add(outputFilePattern);
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
		
		try {
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList("bash",workingDir+File.separator+shFile));
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
