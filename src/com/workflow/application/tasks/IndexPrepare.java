package com.workflow.application.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	    Logger.getLogger(IndexPrepare.class.getName()).log(Level.INFO,"In Index Prepare");

		double random = Math.random() * 10000;
		String outputContigsFile = random + "_"+System.getProperty("Name")+"_contigs.txt";
		String outputDir = random + "_"+System.getProperty("Name");
		List<String> inputFiles = new ArrayList<>();
		List<String> outfiles=new ArrayList<String>();
		
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
//				outfiles.add(new File(inputFile).getName());
			}
		}
		
		List<FileProperties> resultFiles=null;
		if(inputFiles.size()>1)
			return new Object[]{"FAIL",resultFiles};
		String stagingLocation = helper.getInputLocation();
		
		
		List<String> contigsListCommand = Util.getContigsListCommand(stagingLocation + inputFiles.get(0));
		contigsListCommand.add(workingDir + File.separator + outputContigsFile);
		
		Util.writeShAndStartProcess(contigsListCommand, workingDir, random, "_getContigs.sh");
		
		/*Util.getSplitBamByContigs(outputContigsFile, inputFiles.get(0), outputDir);
		
		File dir= new File(outputDir);
		for(String str:dir.list()){
			File file = new File(str);
			if(!file.isDirectory()){
				outfiles.add(str);
			}
		}*/
		List<String> splitBAMbyChromosome = Util.splitBAMbyChromosome(new File(workingDir + File.separator +outputContigsFile),stagingLocation +  inputFiles.get(0));
		outfiles.addAll(splitBAMbyChromosome);
		List<String> baiFiles = new ArrayList<>();
		for(String splitBam:splitBAMbyChromosome){
			baiFiles.add(splitBam+".bai");
		}
		task.uploadResults(baiFiles, workingDir, helper.getOutputFile());
		
		resultFiles=task.uploadResults(outfiles, workingDir, helper.getOutputFile());
    	return new Object[]{"OK",resultFiles};
	}
	

}
