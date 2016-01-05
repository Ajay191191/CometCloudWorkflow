package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class PrintReadsTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {

		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		String outputBAM = Math.random()*1000+"_"+System.getProperty("Name")+ "_output_recalibrated.bam";
		String stagingLocation = helper.getInputLocation();
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(!inputFile.endsWith(".bai"))
					inputFiles.add(Util.getStagingLocation(stagingLocation,workingDir, inputFile));
//				inputFiles.add(stagingLocation + File.separator + inputFile);
			}
		}
		List<FileProperties> resultFiles=new ArrayList<>();
		if(inputFiles.size()>1 || inputFiles.size()==0){
			return new Object[]{"OK",resultFiles};
		}
		Object calibratedCSV = helper.getNthObjectFromList(3);
		if(calibratedCSV instanceof FileProperties){
			List<String> printReadsCommand = Util.getPrintReadsCommand( inputFiles.get(0), workingDir + File.separator +outputBAM,stagingLocation + File.separator + ((FileProperties)calibratedCSV).getName());
			Util.runProcessWithListOfCommands(printReadsCommand);
			outputFiles.add(outputBAM);
			outputFiles.add(outputBAM.replaceAll(".bam", ".bai"));
		}
		resultFiles =task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
