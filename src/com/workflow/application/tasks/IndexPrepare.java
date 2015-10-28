package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;
import com.workflow.application.util.Util;

public class IndexPrepare implements Task {

	@Override
	public Object[] performTask(InputHelper helper, WorkerTask task) {
		String workingDir = System.getProperty("WorkingDir");
		
		double random = Math.random() * 10000;
		String outputContigsFile = random + "_"+System.getProperty("Name")+"_contigs.txt";
		
		List<String> contigsListCommand = Util.getContigsListCommand(random+ "_"+System.getProperty("Name"));
		contigsListCommand.add(outputContigsFile);
		
		Util.writeShAndStartProcess(contigsListCommand, workingDir, random, "_prepareIndex.sh");
		File dir= new File(random+ "_"+System.getProperty("Name"));
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
