package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
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
		
//		FileProperties inputFileProperties = null;
		
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(!inputFile.endsWith(".bai")){
//					inputFiles.add(stagingLocation + File.separator + inputFile);
//					inputFiles.add(Util.getStagingLocation(stagingLocation,workingDir, inputFile));
//					inputBams.append(" -I "+Util.getStagingLocation(stagingLocation,workingDir, inputFile)+" ");
					inputFiles.add("-I");
					inputFiles.add(Util.getStagingLocation(stagingLocation,workingDir, inputFile));
					//For now delete after adding split bams:
//					outputFiles.add(new File(inputFile).getName());
//					inputFileProperties = helper.getInputFiles().get(0);	//Will have to change this
				}
			}
		}
		
		
		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+"_calibration.csv";
		List<FileProperties> resultFiles=null;
		/*if(inputFiles.size()>1 || inputFiles.size()==0)
			return new Object[]{"OK",resultFiles};*/
		
//		List<String> baseRecalibratorCommand = Util.getBaseRecalibratorCommand( inputFiles.get(0), workingDir + File.separator + outputFile);
		List<String> baseRecalibratorCommand = Util.getBaseRecalibratorCommand( inputFiles, workingDir + File.separator + outputFile);
		Util.runProcessWithListOfCommands(baseRecalibratorCommand);
		inputFiles.removeAll(Collections.singleton("-I"));
		/*List<String> contigsListCommand = Util.getContigsListCommand(inputFiles.get(0));
		contigsListCommand.add(workingDir + File.separator + outputContigsFile);
		
//		Util.runProcessWithListOfCommands(contigsListCommand);
		Util.writeShAndStartProcess(contigsListCommand, workingDir, random, "_getContigs.sh");
		*/
		
		List<String> toUpload = new ArrayList<>();
		resultFiles = new ArrayList<>();
		for(FileProperties inputFile:helper.getInputFiles()){
			if(!inputFile.getName().endsWith(".bai")){
				resultFiles.add(inputFile);
			}
		}
		resultFiles.addAll(task.uploadResults(new ArrayList<>(Arrays.asList(outputFile)), workingDir, helper.getOutputFile()));
		
		
//		resultFiles.addAll();
//		resultFiles.addAll(task.uploadResults(new ArrayList<>(Arrays.asList(outputFile)), workingDir, helper.getOutputFile()));
//		List<String> splitBAMbyChromosome = Util.splitBAMbyChromosome(new File(workingDir + File.separator + outputContigsFile), inputFiles.get(0));
		
//		toUpload.addAll(splitBAMbyChromosome);
//		toUpload.add(outputFile);
//		toUpload.addAll(resultFiles);
//		resultFiles = Util.uploadAndGetResults(helper, task, workingDir, toUpload,1);
		
		/*resultFiles.addAll(task.uploadResults(splitBAMbyChromosome, workingDir, helper.getOutputFile()));
		
		List<String> baiFiles = new ArrayList<>();
		for(String splitBam:splitBAMbyChromosome){
			baiFiles.add(splitBam+".bai");
		}
		task.uploadResults(baiFiles, workingDir, helper.getOutputFile());
		*/
		
//		resultFiles.add(inputFileProperties);
		return new Object[]{"OK",resultFiles};
	}

}