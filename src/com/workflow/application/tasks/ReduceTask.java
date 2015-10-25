package com.workflow.application.tasks;

import java.util.ArrayList;
import java.util.List;

import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;

public class ReduceTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

		List outfiles=new ArrayList();
		for(String location: helper.getInputsHash().keySet()){
			List<String> files = helper.getInputsHash().get(location);
			for(String inputFile:files){
				/*try(Scanner reader = new Scanner(new File(workingdir,inputFile))){
					sum += reader.nextInt();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				outfiles.add(inputFile);
			}
		}
		
		List<FileProperties> resultFiles=task.uploadResults(outfiles,System.getProperty("WorkingDir") , helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
