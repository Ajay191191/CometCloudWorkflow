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

		String stagingLocation = helper.getInputLocation();
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(!inputFile.endsWith(".bai")){
					String input = Util.getStagingLocation(stagingLocation, workingDir, inputFile);
					inputFiles.add(input);
					if(!Util.ifIndexExistsForBAM(input)){
						Util.indexBAM(input);
					}
				}
//				outfiles.add(new File(inputFile).getName());
			}
		}
		
		List<FileProperties> resultFiles=new ArrayList<>();
		if(inputFiles.size()>1 || inputFiles.size()==0)
			return new Object[]{"OK",resultFiles};
		
		List<String> contigsListCommand = Util.getContigsListCommand(Util.getStagingLocation(stagingLocation, workingDir, inputFiles.get(0)));
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
		List<String> splitBAMbyChromosome = Util.splitBAMbyChromosome(new File(workingDir + File.separator +outputContigsFile),Util.getStagingLocation(stagingLocation, workingDir, inputFiles.get(0)));
		outfiles.addAll(splitBAMbyChromosome);
//		List<String> baiFiles = new ArrayList<>();
		/*for(String splitBam:splitBAMbyChromosome){
			outfiles.add(splitBam+".bai");
		}*/
//		task.uploadResults(baiFiles, workingDir, helper.getOutputFile());
		
		resultFiles=task.uploadResults(outfiles, workingDir, helper.getOutputFiles().get(0));
    	return new Object[]{"OK",resultFiles};
	}
	

}
