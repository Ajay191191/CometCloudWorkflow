package com.workflow.application.tasks.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PoolFactory<T>{
	
	List<Worker<T>> workers;
	List<T> output = new ArrayList<>();

	
	public List<Worker<T>> getWorkers() {
		return workers;
	}


	public void setWorkers(List<Worker<T>> workers) {
		this.workers = workers;
	}


	public List<T> getOutput() {
		return output;
	}


	public void setOutput(List<T> output) {
		this.output = output;
	}


	public PoolFactory(List<Worker<T>> workers) {
		super();
		this.workers = workers;
	}


	public List<T> runAndGetResults() throws InterruptedException, ExecutionException{
		ExecutorService threadExecutor = Executors.newFixedThreadPool(20);
		List<Future<T>> allResults = threadExecutor.invokeAll(this.workers);
		for(Future<T> result : allResults){
			output.add(result.get());
		}
		threadExecutor.shutdown();
		threadExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		return output;
	}
}