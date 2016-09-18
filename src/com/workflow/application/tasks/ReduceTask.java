package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import picard.sam.markduplicates.MarkDuplicates;
import tassl.application.cometcloud.FileProperties;

public class ReduceTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

		long time1=System.currentTimeMillis();
		long fileSize = 0;
		
		String workingDir = System.getProperty("WorkingDir");
		List outfiles=new ArrayList();
		List<String> parameters=new ArrayList();
//		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+"_mdup.bam";
		String outputFile = helper.getOutputFiles().get(0).getName();
		String storageLocation = null;
		String stagingLocation = helper.getInputLocation();
		System.out.println("Location " + stagingLocation);
		List<String> indexCommand = Util.getIndexCommand();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				String inputStage = Util.getStagingLocation(stagingLocation, workingDir, inputFile);
				parameters.add("INPUT="+inputStage);
				fileSize+=new File(inputStage).length();
			}
			parameters.add("OUTPUT="+workingDir+File.separator+outputFile);
			parameters.add("REMOVE_DUPLICATES=true");
			parameters.add("METRICS_FILE="+workingDir+File.separator+"metrics.txt");
			//INPUT=$SORTEDBAMFILENAME OUTPUT=$MARKDUPLICATESBAM REMOVE_DUPLICATES=false METRICS_FILE=metrics.txt
			new MarkDuplicates().instanceMain(parameters.toArray(new String[0]));
			indexCommand.add(workingDir+File.separator+outputFile);
			Util.runProcessWithListOfCommands(indexCommand);
			outfiles.add(outputFile);
//			outfiles.add(outputFile+".bai");
			
		}
		
		List<FileProperties> resultFiles=task.uploadResults(outfiles,workingDir, helper.getOutputFiles().get(0));
		Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Time for Mark duplicates"+(System.currentTimeMillis() - time1) + " Input file size " + fileSize );
		return new Object[]{"OK",resultFiles};
	
	}

}
