package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.tasks.worker.PoolFactory;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class BaseRecalibratorTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {

		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		double random = Math.random() * 10000;
		
		String outputContigsFile = random + "_"+System.getProperty("Name")+"_contigs.txt";
		String stagingLocation = helper.getInputLocation();
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(stagingLocation + File.separator + inputFile);
			}
		}
		
		
		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+"_calibration.csv";
		List<FileProperties> resultFiles=null;
		if(inputFiles.size()>1)
			return new Object[]{"FAIL",resultFiles};
		
		List<String> baseRecalibratorCommand = Util.getBaseRecalibratorCommand( inputFiles.get(0), workingDir + File.separator + outputFile);
		Util.runProcessWithListOfCommands(baseRecalibratorCommand);
		
		List<String> contigsListCommand = Util.getContigsListCommand(inputFiles.get(0));
		contigsListCommand.add(outputContigsFile);
		
		Util.runProcessWithListOfCommands(contigsListCommand);
		
		outputFiles.add(outputFile);
		outputFiles.addAll(Util.splitBAMbyChromosome(new File(outputContigsFile), inputFiles.get(0)));
		resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	}
	
	
}