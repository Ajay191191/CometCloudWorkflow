package com.workflow.application.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.workflow.application.tasks.BaseRecalibratorTask;
import com.workflow.application.tasks.HaplotypeCallerTask;
import com.workflow.application.tasks.IndexPrepare;
import com.workflow.application.tasks.IndexTask;
import com.workflow.application.tasks.MapTask;
import com.workflow.application.tasks.PrepareBaseRecalibrator;
import com.workflow.application.tasks.PrintReadsTask;
import com.workflow.application.tasks.RealignerTargetCreatorTask;
import com.workflow.application.tasks.ReduceTask;
import com.workflow.application.tasks.Task;
import com.workflow.application.util.HelperConstants;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.WorkflowTaskTuple;

public class InputHelper{
	private String method;
	private List<FileProperties> outputFiles;
	private List<FileProperties> inputFiles;
	private HashMap <String,List>inputsHash;
	private WorkflowTaskTuple tasktuple;
	private String inputLocation;
	private List data;
	
	
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
	public List<FileProperties> getInputFiles() {
		return inputFiles;
	}
	public void setInputFiles(List<FileProperties> inputFiles) {
		this.inputFiles = inputFiles;
	}
	public Object getNthObjectFromList(int n){
		return data.get(n);
	}
	public InputHelper(String method, List<FileProperties> outputFiles,List<FileProperties> inputFiles,WorkflowTaskTuple tasktuple) {
		super();
		this.method = method;
		this.setOutputFiles(outputFiles);
		this.inputFiles = inputFiles;
		this.tasktuple = tasktuple;
		this.calculateInputsHash();
	}
	public InputHelper(List data, WorkflowTaskTuple tasktuple) {
		this((String)data.get(0), (List<FileProperties>) data.get(1), (List<FileProperties>)data.get(2),tasktuple);
		this.data = data;
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
		if (this.method.equals(HelperConstants.MAP))
			return new MapTask();
		else if (this.method.equals(HelperConstants.REDUCE)) {
			return new ReduceTask();
		}else if (this.method.equals(HelperConstants.INDEX_PREPARE)) {
			return new IndexPrepare();
		}else if (this.method.equals(HelperConstants.INDEX)) {
			return new IndexTask();
		}else if (this.method.equals(HelperConstants.REALIGNERTARGETCREATOR)) {
			return new RealignerTargetCreatorTask();
		}else if (this.method.equals(HelperConstants.PREPAREBASERECALIBRATOR)) {
			return new PrepareBaseRecalibrator();
		}else if (this.method.equals(HelperConstants.BASERECALIBRATOR)) {
			return new BaseRecalibratorTask();
		}else if (this.method.equals(HelperConstants.PRINTREADS)) {
			return new PrintReadsTask();
		}else if (this.method.equals(HelperConstants.HAPLOTYPECALLER)) {
			return new HaplotypeCallerTask();
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
		return "Method: " + this.method + " Input: " + this.inputFiles + " Output: " + this.getOutputFiles() + " TaskTuple: " + this.tasktuple;
	}
	public String getInputLocation() {
		return inputLocation;
	}
	public void setInputLocation(String inputLocation) {
		this.inputLocation = inputLocation;
	}
	public List<FileProperties> getOutputFiles() {
		return outputFiles;
	}
	public void setOutputFiles(List<FileProperties> outputFiles) {
		this.outputFiles = outputFiles;
	}
}