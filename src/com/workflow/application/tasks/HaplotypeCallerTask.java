package com.workflow.application.tasks;

import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class HaplotypeCallerTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		String outputvcf = Math.random()*1000+"_"+System.getProperty("Name")+ "_index.sh";
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
			}
		}
		List<FileProperties> resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		if(inputFiles.size()>1){
			return new Object[]{"FAIL",resultFiles};
		}
		List<String> haplotypeCallerCommand = Util.getHaplotypeCallerCommand(20, inputFiles.get(0), outputvcf);
		Util.runProcessWithListOfCommands(haplotypeCallerCommand);
		outputFiles.add(outputvcf);
		
		return new Object[]{"OK",resultFiles};
	}

}
