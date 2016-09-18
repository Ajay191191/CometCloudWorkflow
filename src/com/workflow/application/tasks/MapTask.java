package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class MapTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

//		Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Vars: "  + System.getProperty("bwaExecutable") + System.getProperty("samtoolsExecutable") + System.getProperty("gatkJar") + System.getProperty("referenceFastqFile") + System.getProperty("dbsnpFile"));
		
		Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Starting BWA");
		long time1 = System.currentTimeMillis();
		long fileSize=0;
		Logger.getLogger(MapTask.class.getName()).log(Level.INFO,"Start BWA : " + System.currentTimeMillis());
		String workingdir = System.getProperty("WorkingDir");
    	List<String> command = Util.getBWACommand(helper.getTasktuple().getTaskid()+"_"+System.getProperty("Name"));
    	
    	List<String> outfiles=new ArrayList<String>();
    	for(String location: helper.getInputsHash().keySet()){
    		List<String> files = helper.getInputsHash().get(location);
    		for(String inputFile:files){
    			System.out.println("InputFile: "+inputFile);
    			fileSize = new File(workingdir+File.separator+inputFile).length();
    			command.add(workingdir+File.separator+inputFile);
    		}
    	}
    	
    	double random = Math.random() * 10000;
//		String outputFile = random + "_"+System.getProperty("Name");
    	String outputFile = helper.getOutputFiles().get(0).getName();
 
		command.addAll(Util.getPipeSortCommand());
    	command.add(workingdir + File.separator + outputFile.replaceAll(".bam", ""));
    	
    	outfiles.add(outputFile/*+".bam"*/);	//To keep the naming consistent as samtools appends its own bam extension. 
    	
    	if(Util.writeShAndStartProcess(command,workingdir,random,"_bwa.sh") && Util.getFileSize(workingdir+File.separator+outputFile/*+".bam"*/)>100){
    		List<FileProperties> resultFiles=task.uploadResults(outfiles, workingdir, helper.getOutputFiles().get(0));
    		Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Time for BWA"+(System.currentTimeMillis() - time1) + " Input file size" + fileSize);
    		return new Object[]{"OK",resultFiles};
    	}
    	Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Time for BWA"+(System.currentTimeMillis() - time1) + " Input file size " + fileSize );
    	return new Object[]{"ERROR: Fail",null};
	
	}
}
