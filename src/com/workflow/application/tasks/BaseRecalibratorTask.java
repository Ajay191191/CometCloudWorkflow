package com.workflow.application.tasks;

import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class BaseRecalibratorTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {

		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
			}
		}
		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+"_calibration.csv";
		List<FileProperties> resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		if(inputFiles.size()>1)
			return new Object[]{"FAIL",resultFiles};
		
		List<String> baseRecalibratorCommand = Util.getBaseRecalibratorCommand(20, inputFiles.get(0), outputFile);
		Util.runProcessWithListOfCommands(baseRecalibratorCommand);
		outputFiles.add(outputFile);
		return new Object[]{"OK",resultFiles};
	
	}

}
