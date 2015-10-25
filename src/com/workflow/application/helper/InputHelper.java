package com.workflow.application.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.WorkflowTaskTuple;

import com.workflow.application.tasks.MapTask;
import com.workflow.application.tasks.ReduceTask;
import com.workflow.application.tasks.Task;
import com.workflow.application.util.HelperConstants;

public class InputHelper{
	private String method;
	private FileProperties outputFile;
	private List<FileProperties> inputFiles;
	private HashMap <String,List>inputsHash;
	private WorkflowTaskTuple tasktuple;
	
	public HashMap<String, List> getInputsHash() {
		return inputsHash;
	}
	public void setInputsHash(HashMap<String, List> inputsHash) {
		this.inputsHash = inputsHash;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public FileProperties getOutputFile() {
		return outputFile;
	}
	public void setOutputFile(FileProperties outputFile) {
		this.outputFile = outputFile;
	}
	public List<FileProperties> getInputFiles() {
		return inputFiles;
	}
	public void setInputFiles(List<FileProperties> inputFiles) {
		this.inputFiles = inputFiles;
	}
	public InputHelper(String method, FileProperties outputFile,List<FileProperties> inputFiles,WorkflowTaskTuple tasktuple) {
		super();
		this.method = method;
		this.outputFile = outputFile;
		this.inputFiles = inputFiles;
		this.tasktuple = tasktuple;
		this.calculateInputsHash();
	}
	public HashMap<String, List> calculateInputsHash() {
		inputsHash = new HashMap<String, List>();
		for (FileProperties fp:this.inputFiles){
	        List temp=inputsHash.get(fp.getLocation());
	        if (temp==null){
	            temp=new ArrayList();
	            inputsHash.put(fp.getLocation(), temp);
	        }
	        temp.add(fp.getName());
	    }
		return inputsHash;
	}
	
	public Task getTask(){
		if (this.method.equals(HelperConstants.MAP)) {
			return new MapTask();
		} else if (this.method.equals(HelperConstants.REDUCE)) {
			return new ReduceTask();
		}
		return null;
	}
	public WorkflowTaskTuple getTasktuple() {
		return tasktuple;
	}
	public void setTasktuple(WorkflowTaskTuple tasktuple) {
		this.tasktuple = tasktuple;
	}
	
	@Override
	public String toString() {
		return "Method: " + this.method + " Input: " + this.inputFiles + " Output: " + this.outputFile + " TaskTuple: " + this.tasktuple;
	}
}