package com.workflow.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.util.HelperConstants;
import com.workflow.application.util.Util;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.GenerateTasksAbstract;
import tassl.application.cometcloud.GenerateTasksObject;

public class GeneratorTask extends GenerateTasksAbstract {

	@Override
	public GenerateTasksObject createTasks(String workflowId,String stageId, List<FileProperties> input, FileProperties output,String propertyFileValues, List dependencies, String method){
	    GenerateTasksObject taskObj=null;
	    this.loadProperties(propertyFileValues);
	    Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"method " + method);
	    if(method.equals(HelperConstants.MAP)){
	        taskObj=map(input,output,propertyFileValues,method,stageId,workflowId);
	    }else if(method.equals(HelperConstants.REDUCE)
	    		|| method.equals(HelperConstants.PREPAREBASERECALIBRATOR) || method.equals(HelperConstants.BASERECALIBRATOR)){
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=reduce(input,output,propertyFileValues,previousFiles,method,stageId,workflowId);
	    }else if(method.equals(HelperConstants.INDEX_PREPARE) || method.equals(HelperConstants.REALIGNERTARGETCREATOR) ||/* method.equals(HelperConstants.HAPLOTYPECALLER)|| */method.equals(HelperConstants.INDEX) ){
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=createNtoNTasks(input,output,propertyFileValues,previousFiles,method,stageId,workflowId);
	    }else if(method.equals(HelperConstants.HAPLOTYPECALLER)){
	    	Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"In condition method " + method);
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=createTaskForHaplotype(input,output,propertyFileValues,previousFiles,method,stageId,workflowId);
	    }
	    return taskObj;
	}

	public GenerateTasksObject map(List<FileProperties> input, FileProperties output, String propertyFile, String method,String stageID, String workflowId){
	    int taskid=0;

	    List <Double> minTime= new ArrayList<Double>();
	    List <Double> maxTime=new ArrayList<Double>();
	    List <List>taskParams=new ArrayList<List>();
	    List <String>taskRequirement=new ArrayList<String>();
	    List <List<FileProperties>>inputList=new ArrayList<List<FileProperties>>();
	    List <List<FileProperties>> outputList=new ArrayList<>();

	    double minTimeVal=Double.parseDouble(getProperty("minTime"));
	    double maxTimeVal=Double.parseDouble(getProperty("maxTime"));

	    for(FileProperties inputFP: input){

	    	String inputS=inputFP.getLocation();
	        String []parts=inputS.split(":");
	        String returnSsh=executeSsh(parts[0]," ls -l "+parts[1]+" | awk '{print $5, $9}'");
	        String [] files=returnSsh.split("\n"); //returns size and name

	        for(int i=0;i<files.length;i++){
	            if(!files[i].trim().isEmpty()){
	                List <FileProperties>inputs=new ArrayList();
	                List <FileProperties>outputs=new ArrayList();
	                String [] fileParts=files[i].split(" ");//size and name
	                if(fileParts[1].contains("_2_"))
	                	continue;
	                inputs.add(new FileProperties(fileParts[1],inputS,Double.parseDouble(fileParts[0]),inputFP.getZone(), inputFP.getSitename(), inputFP.getConstraints()));
	                inputs.add(new FileProperties(fileParts[1].replace("_1_", "_2_"),inputS,Double.parseDouble(fileParts[0]),inputFP.getZone(), inputFP.getSitename(), inputFP.getConstraints()));
	                
	                String outputFileSuffix = fileParts[1].split("_")[0];
	                
	                FileProperties outFP= new FileProperties("outputfile." + workflowId + "." + stageID + "." + outputFileSuffix, output.getLocation(), Double.parseDouble(fileParts[0]), output.getZone(), output.getSitename(), output.getConstraints());
                    outputs.add(outFP);
	                
	                double taskDuration=minTimeVal + (Math.random() * (maxTimeVal - minTimeVal));
	                taskParams.add(taskid, Arrays.asList(method,output,inputs,taskDuration));
	                taskRequirement.add("large");
	                minTime.add(minTimeVal);
	                maxTime.add(maxTimeVal);
	                inputList.add(inputs);
	                outputList.add(outputs);
	                taskid++;

	            }
	        }

	    }
	    return new GenerateTasksObject(taskParams,taskRequirement, minTime, maxTime,inputList,outputList);
	}

	public GenerateTasksObject reduce(List<FileProperties> input,FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method,String stageID, String workflowId) {

		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();
		List<FileProperties> inputs = new ArrayList();
		
		List<List<FileProperties>> outputList = new ArrayList();
		List<FileProperties> outputs = new ArrayList();
		
		for (String key : previousResults.keySet()) {
			inputs.add(previousResults.get(key));
		}
		inputList.add(inputs);
		double minTimeVal = Double.parseDouble(getProperty("minTime"));
		double maxTimeVal = Double.parseDouble(getProperty("maxTime"));
		taskParams.add(taskid, Arrays.asList(method,output, inputs));
		taskRequirement.add("large"); // Requirement
		minTime.add(minTimeVal);
		maxTime.add(maxTimeVal);
		inputList.add(inputs);
		taskid++;

		//TODO:Get number of contigs from the reference file.
		if(method.equals(HelperConstants.BASERECALIBRATOR)){
			outputs.add(new FileProperties("reduce."+workflowId+"."+stageID+".0"+"_calibration.csv",output.getLocation(),123.5, output.getZone(), output.getSitename(), output.getConstraints()));
			for(int i=0;i<94;i++){
				outputs.add(new FileProperties("index_prepared."+workflowId+"."+stageID+"."+taskid+"_contig_"+i,output.getLocation(),123.5, output.getZone(), output.getSitename(), output.getConstraints()));
				taskid++;
			}
		}
		else
			outputs.add(new FileProperties("reduce."+workflowId+"."+stageID+".0",output.getLocation(),123.5, output.getZone(), output.getSitename(), output.getConstraints()));
		outputList.add(outputs);
		
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, outputList);
	}

	public GenerateTasksObject createNtoNTasks(List<FileProperties> input, FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method,String stageID, String workflowId){

		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();
		List<List<FileProperties>> outputList = new ArrayList();
		
		double minTimeVal = Double.parseDouble(getProperty("minTime"));
		double maxTimeVal = Double.parseDouble(getProperty("maxTime"));

		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"Previous Results " + previousResults.keySet());
		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"Previous set " + Util.NtoNTasks);
		for (String key : previousResults.keySet()) {
			List<FileProperties> inputs = new ArrayList();
			List<FileProperties> outputs = new ArrayList();
			/*if(Util.NtoNTasks.contains(previousResults.get(key)))
				continue;
			Util.NtoNTasks.add(previousResults.get(key));*/
			inputs.add(previousResults.get(key));
			inputList.add(inputs);
			taskParams.add(taskid, Arrays.asList(method,output, inputs));
			taskRequirement.add("large"); // Requirement
			minTime.add(minTimeVal);
			maxTime.add(maxTimeVal);
//			inputList.add(inputs);
			if(!method.equals(HelperConstants.INDEX_PREPARE)){
				outputs.add(new FileProperties("outputfile."+workflowId+"."+stageID+"."+taskid,output.getLocation(),123.6, output.getZone(), output.getSitename(), output.getConstraints()));
				outputList.add(outputs);
			}
			taskid++;
		}
		
		//TODO: Get contig list from the reference file.
		if(method.equals(HelperConstants.INDEX_PREPARE)){
			List<FileProperties> outputs = new ArrayList();
			for(int i=0;i<94;i++){
				outputs.add(new FileProperties("index_prepared."+workflowId+"."+stageID+"."+taskid+"_contig_"+i,output.getLocation(),123.5, output.getZone(), output.getSitename(), output.getConstraints()));
				taskid++;
			}
			outputList.add(outputs);
		}
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, outputList);
	}

	public GenerateTasksObject createTaskForHaplotype(List<FileProperties> input, FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method,String stageID, String workflowId){

		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"In method method " + method);
		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();
		List<List<FileProperties>> outputList = new ArrayList();
		double minTimeVal = Double.parseDouble(getProperty("minTime"));
		double maxTimeVal = Double.parseDouble(getProperty("maxTime"));

		FileProperties calibratedCSVFile = null;
		for (String key : previousResults.keySet()) {
			FileProperties inputFile = previousResults.get(key);
			if(inputFile.getName().contains("_calibration.csv")){
				calibratedCSVFile = inputFile;
				break;
			}
		}

		for (String key : previousResults.keySet()) {
			List<FileProperties> inputs = new ArrayList();
			List<FileProperties> outputs = new ArrayList();
			FileProperties inputFile = previousResults.get(key);
			if(inputFile.getName().contains("_calibration.csv")){
				continue;
			}
			inputs.add(calibratedCSVFile);
			inputs.add(inputFile);
			inputList.add(inputs);
			taskParams.add(taskid, Arrays.asList(method,output, inputs,calibratedCSVFile));
			taskRequirement.add("large"); // Requirement
			minTime.add(minTimeVal);
			maxTime.add(maxTimeVal);
			inputList.add(inputs);
			outputs.add(new FileProperties("outputfile."+workflowId+"."+stageID+"."+taskid+".vcf",output.getLocation(),123.5, output.getZone(), output.getSitename(), output.getConstraints()));
			outputList.add(outputs);
			taskid++;
		}
		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"return method " + method);
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, null);
	}
}
