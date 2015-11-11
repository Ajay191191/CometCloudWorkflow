package com.workflow.application.tasks;

import java.io.File;
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
		String outputvcf = Math.random()*1000+"_"+System.getProperty("Name")+ "_output.vcf";
		String stagingLocation = helper.getInputLocation();
		
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				if(!inputFile.endsWith(".bai"))
					inputFiles.add(stagingLocation+File.separator + inputFile);
			}
		}
		List<FileProperties> resultFiles=null;
		if(inputFiles.size()>1){
			return new Object[]{"FAIL",resultFiles};
		}
		List<String> haplotypeCallerCommand = Util.getHaplotypeCallerCommand( inputFiles.get(0), workingDir + File.separator +outputvcf);
		Util.runProcessWithListOfCommands(haplotypeCallerCommand);
		outputFiles.add(outputvcf);
		
		resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	}

}
