package com.workflow.application;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.helper.InputHelper;
import com.workflow.application.tasks.Task;

import tassl.application.cometcloud.FileProperties;
import tassl.application.cometcloud.WorkflowMeteorGenericWorker;
import tassl.application.cometcloud.WorkflowTaskTuple;

public class WorkerTask extends WorkflowMeteorGenericWorker {

	@Override
	public Object computeTaskSpecific(Object dataobj, WorkflowTaskTuple tasktuple) {
	    Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO, "WorkerTask "+this.getPeerIP()+" gets taskid " + tasktuple.getTaskid());
	    
	    InputHelper inputHelper = new InputHelper((List)dataobj,tasktuple);
	    Logger.getLogger(WorkerTask.class.getName()).log(Level.INFO,"" + inputHelper);
	    
	    String workingdir=System.getProperty("WorkingDir");
	    HashMap <String,List>inputsHash= inputHelper.getInputsHash();
	    
	    //retrieve all input files, we make a call per data source
	    for(String site:inputsHash.keySet()){
	        //method that retrieve input files and place them on working dir
	        String status=this.getFile(true, site, inputsHash.get(site), workingdir);;
	    }
	    
	    Task task = inputHelper.getTask();
	    
	    return task.performTask(inputHelper,this);
	}

	//this function is not yet supported, we would leave it as follows.
	@Override
	public void cancelJob() {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public List<FileProperties> uploadResults(List<String> arg0,String arg1, FileProperties arg2) {
		return super.uploadResults(arg0, arg1, arg2);
	}
}
