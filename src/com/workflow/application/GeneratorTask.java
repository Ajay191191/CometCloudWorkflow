package com.workflow.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.tasks.IndexPrepare;
import com.workflow.application.util.HelperConstants;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.GenerateTasksAbstract;
import tassl.application.cometcloud.GenerateTasksObject;

public class GeneratorTask extends GenerateTasksAbstract {

	
	@Override
	public GenerateTasksObject createTasks(String stageId, List<FileProperties> input, FileProperties output,String propertyFileValues, List dependencies, String method){
	    GenerateTasksObject taskObj=null;
	    this.loadProperties(propertyFileValues);
	    Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"method " + method);
		  
	    if(method.equals(HelperConstants.MAP)){
	        taskObj=map(input,output,propertyFileValues,method);
	    }else if(method.equals(HelperConstants.REDUCE) 
	    		|| method.equals(HelperConstants.PREPAREBASERECALIBRATOR) || method.equals(HelperConstants.BASERECALIBRATOR)){
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=reduce(input,output,propertyFileValues,previousFiles,method);
	    }else if(method.equals(HelperConstants.INDEX_PREPARE) || method.equals(HelperConstants.REALIGNERTARGETCREATOR) || method.equals(HelperConstants.HAPLOTYPECALLER)|| method.equals(HelperConstants.INDEX) ){
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=createNtoNTasks(input,output,propertyFileValues,previousFiles,method);
	    }else if(method.equals(HelperConstants.PRINTREADS)){
	    	Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"In condition method " + method);
	        HashMap <String,FileProperties>previousFiles=this.generatePreviousResultFiles(stageId, dependencies);
	        taskObj=createTaskForPrintReads(input,output,propertyFileValues,previousFiles,method);
	    }
	    return taskObj;
	}
	
	public GenerateTasksObject map(List<FileProperties> input, FileProperties output, String propertyFile, String method){
	    int taskid=0;

	    List <Double> minTime= new ArrayList<Double>();
	    List <Double> maxTime=new ArrayList<Double>();
	    List <List>taskParams=new ArrayList<List>();
	    List <String>taskRequirement=new ArrayList<String>();
	    List <List<FileProperties>>inputList=new ArrayList<List<FileProperties>>();

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
	                String [] fileParts=files[i].split(" ");//size and name
	                if(fileParts[1].contains("_2_"))
	                	continue;
	                inputs.add(new FileProperties(fileParts[1],inputS,Double.parseDouble(fileParts[0]),inputFP.getZone(), inputFP.getSitename(), inputFP.getConstraints()));
//	                inputs.add(new FileProperties(fileParts[1].replace("_1_", "_2_"),inputS,Double.parseDouble(fileParts[0]),inputFP.getZone(), inputFP.getSitename(), inputFP.getConstraints()));
	                double taskDuration=minTimeVal + (Math.random() * (maxTimeVal - minTimeVal));
	                taskParams.add(taskid, Arrays.asList(method,output,inputs,taskDuration));
	                taskRequirement.add("large");
	                minTime.add(minTimeVal);
	                maxTime.add(maxTimeVal);
	                inputList.add(inputs);
	                taskid++;
	                
	            }
	        }

	    }
	    return new GenerateTasksObject(taskParams,taskRequirement, minTime, maxTime,inputList,null);
	}
	
	public GenerateTasksObject reduce(List<FileProperties> input,FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method) {
		
		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();

		List<FileProperties> inputs = new ArrayList();
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
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, null);
	}

	public GenerateTasksObject createNtoNTasks(List<FileProperties> input, FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method){
		
		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();
		double minTimeVal = Double.parseDouble(getProperty("minTime"));
		double maxTimeVal = Double.parseDouble(getProperty("maxTime"));
		
		
		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"Previous Results " + previousResults.keySet());
			
		for (String key : previousResults.keySet()) {
			List<FileProperties> inputs = new ArrayList();
			inputs.add(previousResults.get(key));
			inputList.add(inputs);
			taskParams.add(taskid, Arrays.asList(method,output, inputs));
			taskRequirement.add("large"); // Requirement
			minTime.add(minTimeVal);
			maxTime.add(maxTimeVal);
			inputList.add(inputs);
			taskid++;
		}
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, null);
	}
	
	public GenerateTasksObject createTaskForPrintReads(List<FileProperties> input, FileProperties output, String propertyFile,HashMap<String, FileProperties> previousResults,String method){
		
		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"In method method " + method);
		int taskid = 0;
		List<Double> minTime = new ArrayList();
		List<Double> maxTime = new ArrayList();
		List<List> taskParams = new ArrayList();
		List<String> taskRequirement = new ArrayList<String>();
		List<List<FileProperties>> inputList = new ArrayList();
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
			FileProperties inputFile = previousResults.get(key);
			if(inputFile.getName().contains("_calibration.csv")){
				continue;
			}
			inputs.add(inputFile);
			inputList.add(inputs);
			taskParams.add(taskid, Arrays.asList(method,output, inputs,calibratedCSVFile));
			taskRequirement.add("large"); // Requirement
			minTime.add(minTimeVal);
			maxTime.add(maxTimeVal);
			inputList.add(inputs);
			taskid++;
		}
		Logger.getLogger(GeneratorTask.class.getName()).log(Level.INFO,"return method " + method);
		return new GenerateTasksObject(taskParams, taskRequirement, minTime,maxTime, inputList, null);
	}
}
