package com.workflow.application.tasks;

import com.workflow.application.WorkerTask;
import com.workflow.application.helper.InputHelper;

public interface Task {

	public Object[] performTask(InputHelper helper,WorkerTask task);
}
