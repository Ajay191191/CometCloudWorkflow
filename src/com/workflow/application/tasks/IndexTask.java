package com.workflow.application.tasks;

import java.util.ArrayList;
import java.util.List;

import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

public class IndexTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		
		String workingDir = System.getProperty("WorkingDir");
		List<String> indexCommand = Util.getIndexCommand();
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
				outputFiles.add(inputFile+".bai");
			}
		}
		indexCommand.addAll(inputFiles);
//		Util.writeShAndStartProcess(indexCommand, workingDir, Math.random()*1000, "_index.sh");
		Util.runProcessWithListOfCommands(indexCommand);
		List<FileProperties> resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	}

}
