package com.workflow.application.tasks.worker;

import java.util.concurrent.Callable;

public interface Worker<T> extends Callable<T>{

}
