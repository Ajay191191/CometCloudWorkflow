package com.workflow.application.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.workflow.application.tasks.IndexPrepare;
import com.workflow.application.tasks.IndexTask;
import com.workflow.application.tasks.MapTask;
import com.workflow.application.tasks.PrepareBaseRecalibrator;
import com.workflow.application.tasks.RealignerTargetCreatorTask;
import com.workflow.application.tasks.ReduceTask;
import com.workflow.application.tasks.Task;
import com.workflow.application.util.HelperConstants;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.WorkflowTaskTuple;

public class InputHelper{
	private String method;
	private FileProperties outputFile;
	private List<FileProperties> inputFiles;
	private HashMap <String,List>inputsHash;
	private WorkflowTaskTuple tasktuple;
	private String inputLocation;
	
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
			if(getInputLocation()==null){
				String[] split = fp.getLocation().split(":");
				setInputLocation(split.length>0?split[1]:fp.getLocation());
			}
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
		}else if (this.method.equals(HelperConstants.INDEX_PREPARE)) {
			return new IndexPrepare();
		}else if (this.method.equals(HelperConstants.INDEX)) {
			return new IndexTask();
		}else if (this.method.equals(HelperConstants.REALIGNERTARGETCREATOR)) {
			return new RealignerTargetCreatorTask();
		}else if (this.method.equals(HelperConstants.PREPAREBASERECALIBRATOR)) {
			return new PrepareBaseRecalibrator();
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
	public String getInputLocation() {
		return inputLocation;
	}
	public void setInputLocation(String inputLocation) {
		this.inputLocation = inputLocation;
	}
}