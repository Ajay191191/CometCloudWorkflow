package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import picard.sam.markduplicates.MarkDuplicates;
import tassl.application.cometcloud.FileProperties;

public class ReduceTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

		String workingDir = System.getProperty("WorkingDir");
		List outfiles=new ArrayList();
		List<String> parameters=new ArrayList();
		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+"_mdup.bam";
		String storageLocation = null;
		String stagingLocation = helper.getInputLocation();
		System.out.println("Location " + stagingLocation);
		List<String> indexCommand = Util.getIndexCommand();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				parameters.add("INPUT="+Util.getStagingLocation(stagingLocation, workingDir, inputFile));
			}
			parameters.add("OUTPUT="+workingDir+File.separator+outputFile);
			parameters.add("REMOVE_DUPLICATES=true");
			parameters.add("METRICS_FILE="+workingDir+File.separator+"metrics.txt");
			//INPUT=$SORTEDBAMFILENAME OUTPUT=$MARKDUPLICATESBAM REMOVE_DUPLICATES=false METRICS_FILE=metrics.txt
			new MarkDuplicates().instanceMain(parameters.toArray(new String[0]));
			indexCommand.add(workingDir+File.separator+outputFile);
			Util.runProcessWithListOfCommands(indexCommand);
			outfiles.add(outputFile);
			outfiles.add(outputFile+".bai");
			
		}
		
		List<FileProperties> resultFiles=task.uploadResults(outfiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
