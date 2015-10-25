package com.example.application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.WorkflowMeteorGenericWorker;
import tassl.application.cometcloud.WorkflowTaskTuple;

public class WorkerTask extends WorkflowMeteorGenericWorker {

	@Override
	public Object computeTaskSpecific(Object dataobj, WorkflowTaskTuple tasktuple) {
	    Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO, "WorkerTask "+this.getPeerIP()+" gets taskid " + tasktuple.getTaskid());
	    
	    List data = (List) dataobj;

	    String method=(String)data.get(0);
	    
	    FileProperties outputFP=(FileProperties)data.get(1);
	    
	    List <FileProperties> inputs=(List<FileProperties>)data.get(2);
	    
	    String workingdir=System.getProperty("WorkingDir");
	    
	    HashMap <String,List>inputsHash=new HashMap();
	    
	    for (FileProperties fp:inputs){
	        List temp=inputsHash.get(fp.getLocation());
	        if (temp==null){
	            temp=new ArrayList();
	            inputsHash.put(fp.getLocation(), temp);
	        }
	        temp.add(fp.getName());
	    }
	    
	    //retrieve all input files, we make a call per data source
	    for(String site:inputsHash.keySet()){
	        //method that retrieve input files and place them on working dir
	        String status=this.getFile(true, site, inputsHash.get(site), workingdir);;
	    }
	    
	    if(method.equals("map")){
	       return map(tasktuple, outputFP, workingdir, inputsHash);
	    }else if (method.equals("reduce")){
	    	return reduce(tasktuple, data, outputFP, workingdir, inputsHash);
	    }
	    return null;

	}

	private Object reduce(WorkflowTaskTuple tasktuple, List data,FileProperties outputFP, String workingdir,HashMap<String, List> inputsHash) {
		
		List outfiles=new ArrayList();
		for(String location: inputsHash.keySet()){
			List<String> files = inputsHash.get(location);
			for(String inputFile:files){
				/*try(Scanner reader = new Scanner(new File(workingdir,inputFile))){
					sum += reader.nextInt();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				outfiles.add(inputFile);
			}
		}
		
		List<FileProperties> resultFiles=this.uploadResults(outfiles, workingdir, outputFP);
		return new Object[]{"OK",resultFiles};
	}

	private Object map(WorkflowTaskTuple tasktuple,FileProperties outputFP, String workingdir,HashMap<String, List> inputsHash) {
		
    	List<String> command = new ArrayList<String>();
    	command.add("/cac/u01/jz362/Workflow/bwa/bwa-0.7.12/bwa");
    	command.add("mem");
    	command.add("-M");
    	command.add("-t");
    	command.add("20");
    	command.add("-R");
    	command.add("\"@RG\\tID:group1\\tSM:SRR622457\\tPL:illumina\\tLB:lib1\\tPU:unit1\"");
    	command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
    	
    	List<String> outfiles=new ArrayList<String>();
    	for(String location: inputsHash.keySet()){
    		List<String> files = inputsHash.get(location);
    		for(String inputFile:files){
    			command.add(workingdir+File.separator+inputFile);
    		}
    	}
    	
    	double random = Math.random() * 10000;
		String outputFile = random + tasktuple.getTaskid()+"_"+System.getProperty("Name")+".sam";
    	
    	command.add(">");
    	command.add(workingdir + File.separator + outputFile);
    	
    	outfiles.add(outputFile);
    	
    	writeShAndStartProcess(command,workingdir,random);
    	
    	List<FileProperties> resultFiles=this.uploadResults(outfiles, workingdir, outputFP);
    	return new Object[]{"OK",resultFiles};
	}

	private void writeShAndStartProcess(List<String> commands,String workingDir, double random){
		StringBuilder builder = new StringBuilder();
		for(String command:commands){
			builder.append(command+" ");
		}
		String shFile = random+"_bwa.sh";
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
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void extracted(InputStream inputStream) {
		try {
			BufferedReader reader =new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ( (line = reader.readLine()) != null) {
				Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Stream "+ line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//this function is not yet supported, we would leave it as follows.
	@Override
	public void cancelJob() {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
