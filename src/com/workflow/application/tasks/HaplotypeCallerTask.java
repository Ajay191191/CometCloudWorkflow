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

public class HaplotypeCallerTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
//		String outputvcf = Math.random()*1000+"_"+System.getProperty("Name")+ "_output.vcf";
		String outputvcf = helper.getOutputFile().getName();
		String stagingLocation = helper.getInputLocation();
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(inputFile.endsWith(".bam")) {
					String input = Util.getStagingLocation(stagingLocation, workingDir, inputFile);
					inputFiles.add(input);
					if(!Util.ifIndexExistsForBAM(input)){
						Util.indexBAM(input);
					}
				}
			}
		}
		List<FileProperties> resultFiles=new ArrayList<>();
		if(inputFiles.size()>1 || inputFiles.size() ==0 ){
			return new Object[]{"OK",resultFiles};
		}
		
		Object calibratedCSV = helper.getNthObjectFromList(3);
		if(calibratedCSV instanceof FileProperties){
			List<String> haplotypeCallerCommand = Util.getHaplotypeCallerCommand( inputFiles.get(0), workingDir + File.separator +outputvcf,Util.getContigForFile(inputFiles.get(0)),Util.getStagingLocation(stagingLocation, workingDir, ((FileProperties)calibratedCSV).getName()));
			Util.runProcessWithListOfCommands(haplotypeCallerCommand);
			outputFiles.add(outputvcf);
			
			resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
			Logger.getLogger(HaplotypeCallerTask.class.getName()).log(Level.INFO,"End Haplotype : " + System.currentTimeMillis());
		}
		return new Object[]{"OK",resultFiles};
		
		
	}

}
