package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

public class MapTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

		
		String workingdir = System.getProperty("WorkingDir");
    	List<String> command = Util.getBWACommand();
    	
    	List<String> outfiles=new ArrayList<String>();
    	for(String location: helper.getInputsHash().keySet()){
    		List<String> files = helper.getInputsHash().get(location);
    		for(String inputFile:files){
    			command.add(workingdir+File.separator+inputFile);
    		}
    	}
    	
    	double random = Math.random() * 10000;
		String outputFile = random + "_"+System.getProperty("Name")+".sam";
    	
    	command.add(">");
    	command.add(workingdir + File.separator + outputFile);
    	
    	outfiles.add(outputFile);
    	
    	Util.writeShAndStartProcess(command,workingdir,random);
    	
    	List<FileProperties> resultFiles=task.uploadResults(outfiles, workingdir, helper.getOutputFile());
    	return new Object[]{"OK",resultFiles};
	
	}

}
