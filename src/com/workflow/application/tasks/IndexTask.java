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

public class IndexTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		
		long time1 = System.currentTimeMillis();
		long fileSize = 0;
		String workingDir = System.getProperty("WorkingDir");
		List<String> indexCommand = Util.getIndexCommand();
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		String stagingLocation = helper.getInputLocation();
		
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(stagingLocation + File.separator + inputFile);
				fileSize+=new File(stagingLocation + File.separator + inputFile).length();
				outputFiles.add(new File(inputFile).getName());	
			}
		}
		indexCommand.addAll(inputFiles);
//		Util.writeShAndStartProcess(indexCommand, workingDir, Math.random()*1000, "_index.sh");
		Util.runProcessWithListOfCommands(indexCommand);
		List<FileProperties> resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFiles().get(0));
		Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"Time for Index"+(System.currentTimeMillis() - time1) + " Input file size " + fileSize );
		return new Object[]{"OK",resultFiles};
	}

}
