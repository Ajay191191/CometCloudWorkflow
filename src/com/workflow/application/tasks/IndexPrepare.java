package com.workflow.application.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.tasks.worker.ContigSplitBAMWorker;
import com.workflow.application.tasks.worker.PoolFactory;
import com.workflow.application.tasks.worker.Worker;
import com.workflow.application.util.Util;

public class IndexPrepare implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		String workingDir = System.getProperty("WorkingDir");
		
		double random = Math.random() * 10000;
		String outputContigsFile = random + "_"+System.getProperty("Name")+"_contigs.txt";
		String outputDir = random + "_"+System.getProperty("Name");
		List<String> inputFiles = new ArrayList<>();
		List<String> outfiles=new ArrayList<String>();
		
		
		List<String> contigsListCommand = Util.getContigsListCommand(random+ "_"+System.getProperty("Name"));
		contigsListCommand.add(outputContigsFile);
		
		Util.runProcessWithListOfCommands(contigsListCommand);
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
			}
		}
		
		List<FileProperties> resultFiles=task.uploadResults(outfiles, workingDir, helper.getOutputFile());
		if(inputFiles.size()>1)
			return new Object[]{"FAIL",resultFiles};
		
		/*Util.getSplitBamByContigs(outputContigsFile, inputFiles.get(0), outputDir);
		
		File dir= new File(outputDir);
		for(String str:dir.list()){
			File file = new File(str);
			if(!file.isDirectory()){
				outfiles.add(str);
			}
		}*/
		outfiles.addAll(Util.splitBAMbyChromosome(new File(outputContigsFile), inputFiles.get(0)));
		
    	return new Object[]{"OK",resultFiles};
	}
	

}
