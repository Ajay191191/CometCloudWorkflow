package com.workflow.application.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import picard.sam.markduplicates.MarkDuplicates;
import tassl.application.cometcloud.FileProperties;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;

public class ReduceTask implements Task{

	@Override
	public Object[] performTask(InputHelper helper,WorkerTask task) {

		String workingDir = System.getProperty("WorkingDir");
		List outfiles=new ArrayList();
		List inputfiles=new ArrayList();
		String outputFile = Math.random()*1000 + "_"+System.getProperty("Name")+".bam";
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
				inputfiles.add(inputFile);
			}
			if(inputfiles.size()==2){
				//INPUT=$SORTEDBAMFILENAME OUTPUT=$MARKDUPLICATESBAM REMOVE_DUPLICATES=false METRICS_FILE=metrics.txt
				new MarkDuplicates().instanceMain(new String[]{"INPUT="+inputfiles.get(0),"INPUT="+inputfiles.get(1),"OUTPUT="+outputFile,"REMOVE_DUPLICATES=false","METRICS_FILE="+workingDir+File.separator+"metrics.txt"});
				outfiles.add(outputFile);
			}
		}
		
		List<FileProperties> resultFiles=task.uploadResults(outfiles,workingDir, helper.getOutputFile());
		return new Object[]{"OK",resultFiles};
	
	}

}
