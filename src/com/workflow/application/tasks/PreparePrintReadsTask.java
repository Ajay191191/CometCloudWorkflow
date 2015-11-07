package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class PreparePrintReadsTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		String workingDir = System.getProperty("WorkingDir");
		
		double random = Math.random() * 10000;
		String outputContigsFile = random + "_"+System.getProperty("Name")+"_contigs.txt";
		String outputDir = random + "_"+System.getProperty("Name");
		
		
		List<String> contigsListCommand = Util.getContigsListCommand(random+ "_"+System.getProperty("Name"));
		contigsListCommand.add(outputContigsFile);
		
		Util.runProcessWithListOfCommands(contigsListCommand);
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				Util.getSplitBamByContigs(outputContigsFile, inputFile, outputDir);
			}
		}
		
		File dir= new File(outputDir);
		List<String> outfiles=new ArrayList<String>();
		for(String str:dir.list()){
			File file = new File(str);
			if(!file.isDirectory()){
				outfiles.add(str);
			}
		}
		List<FileProperties> resultFiles=task.uploadResults(outfiles, workingDir, helper.getOutputFile());
    	return new Object[]{"OK",resultFiles};
		
	
	}

}
