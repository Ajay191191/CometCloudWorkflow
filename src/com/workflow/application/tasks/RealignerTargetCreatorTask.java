package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
			}
		}
		

		List<FileProperties> resultFiles=new ArrayList<>();
		if(inputFiles.size()!=1){
			return new Object[]{"OK",resultFiles};
		}
		
		String randomString = Math.random()*1000 + "_"+System.getProperty("Name");
		String intervalsFile = randomString+"_output.intervals";
		
		String outputBam = randomString+"_realigned.bam";
		
		
		List<String> realignerTargetCreatorCommand = Util.getRealignerTargetCreatorCommand( inputFiles.get(0),  workingDir + File.separator+ intervalsFile);
//		CommandLineGATK.main(realignerTargetCreatorCommand.toArray(new String[0]));
		Util.runProcessWithListOfCommands(realignerTargetCreatorCommand);
		
		List<String> indelRealignerCommand = Util.getIndelRealignerCommand(inputFiles.get(0), Util.getStagingLocation(stagingLocation, workingDir, intervalsFile), workingDir + File.separator + outputBam);
//		CommandLineGATK.main(indelRealignerCommand.toArray(new String[0]));
		Util.runProcessWithListOfCommands(indelRealignerCommand);
		
		outputFiles.add(outputBam);
//		outputFiles.add(outputBam.replaceAll(".bam", ".bai"));
		resultFiles=task.uploadResults(outputFiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
