package com.workflow.application.tasks;

import java.util.ArrayList;
import java.util.List;

import org.broadinstitute.gatk.engine.CommandLineGATK;
import org.broadinstitute.gatk.tools.walkers.indels.RealignerTargetCreator;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;

public class RealignerTargetCreatorTask implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {

		
		String workingDir = System.getProperty("WorkingDir");
		List<String> inputFiles=new ArrayList();
		List<String> outputFiles=new ArrayList();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				inputFiles.add(inputFile);
			}
		}
		

		String randomString = Math.random()*1000 + "_"+System.getProperty("Name");
		String intervalsFile = randomString+"_output.intervals";
		
		String outputBam = randomString+"_output.intervals";
		
		
		List<String> realignerTargetCreatorCommand = Util.getRealignerTargetCreatorCommand(20, inputFiles.get(0), intervalsFile);
		CommandLineGATK.main(realignerTargetCreatorCommand.toArray(new String[0]));

		List<String> indelRealignerCommand = Util.getIndelRealignerCommand(inputFiles.get(0), intervalsFile, outputBam);
		CommandLineGATK.main(indelRealignerCommand.toArray(new String[0]));
		
		outputFiles.add(outputBam);
		List<FileProperties> resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
