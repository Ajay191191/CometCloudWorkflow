package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class PrepareBaseRecalibrator implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {

		String workingDir = System.getProperty("WorkingDir");
		List<String> outfiles=new ArrayList<>();
		String outputBAM = Math.random()*1000 + "_"+System.getProperty("Name")+"_realigned_merged.bam";
		String stagingLocation = helper.getInputLocation();
		System.out.println("Location " + stagingLocation);
		List<String> bamMergeCommand = Util.getBAMMergeCommand(workingDir + File.separator +outputBAM);
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(!inputFile.endsWith(".bai")){
					bamMergeCommand.add(Util.getStagingLocation(stagingLocation,workingDir, inputFile));
				}
			}
		}
		Util.runProcessWithListOfCommands(bamMergeCommand);
		List<String> indexCommand = Util.getIndexCommand();
		indexCommand.add(outputBAM);
		outfiles.add(outputBAM);
		outfiles.add(outputBAM.replaceAll("bam", "bai"));
		List<FileProperties> resultFiles=task.uploadResults(outfiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	}

}
