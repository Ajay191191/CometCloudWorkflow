package com.workflow.scheduler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import tassl.application.cometcloud.TaskProperties;
import tassl.application.workflow.WorkerForScheduler;
import tassl.workflow.WorkflowStage;

/**
 * @author ajay
 *
 */
public class ExternalScheduler implements Runnable{
	
	private int port;
	private String host;
	
	//Schedule for a particular workflow.
	private HashMap<String, HashMap<Pair,WorkerForScheduler>> wfSchedules;
	
	
	private int serverPort;
	private String serverHost;
	
	public String getServerHost() {
		return serverHost;
	}
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public ExternalScheduler(int port, String host) {
		super();
		this.setPort(port);
		this.host = host;
	}
	@Override
	public void run() {
		try {
			String s="asd";
			System.out.println(s.replaceAll("a", ""));
			System.out.println(s);
			wfSchedules = new HashMap<>();
			ServerSocket socket = new ServerSocket(getPort());
			while(true){
				Socket client = socket.accept();
				DataInputStream inputStream = new DataInputStream(client.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(client.getOutputStream());
				
				String command = inputStream.readUTF();
				if(command.equals("sendGraphToExternalScheduler")){
					String wflId=inputStream.readUTF();
					
					this.serverHost = inputStream.readUTF();
					this.serverPort = inputStream.readInt();
					
					int length = inputStream.readInt();              
					byte[] graphbytesdata = new byte[length];
					inputStream.readFully(graphbytesdata); 
					
					length = inputStream.readInt();
					byte[] availableSlotsdata = new byte[length];
					inputStream.readFully(availableSlotsdata);
					
					try {
						WeightedGraph<TaskProperties, DefaultWeightedEdge> workflowGraph = (WeightedGraph<TaskProperties, DefaultWeightedEdge>)programming5.io.Serializer.deserialize(graphbytesdata);
						
						HashMap<String,List<WorkerForScheduler>> allAvailableSlots = (HashMap<String, List<WorkerForScheduler>>) programming5.io.Serializer.deserialize(availableSlotsdata);
						System.out.println("Graph");
						/*for(DefaultWeightedEdge edge:workflowGraph.edgeSet()){
							TaskProperties tsource=workflowGraph.getEdgeSource(edge);
							TaskProperties ttarget=workflowGraph.getEdgeTarget(edge);
							System.out.println((tsource.getTaskId()==-1?"source":tsource.getTaskParam().get(0))+"."+tsource.getTaskId()+" --------"+
									workflowGraph.getEdgeWeight(edge)+"-------->"+(ttarget.getTaskId()==-1?"source":ttarget.getTaskParam().get(0))+"."+ttarget.getTaskId());
						}*/
						
						System.out.println("Global slots");
						System.out.println(allAvailableSlots);
						Object[] data = process(workflowGraph,allAvailableSlots);
						
						//TODO: Currently replacing the old schedule with the new one for a workflow.
						this.wfSchedules.put(wflId, (HashMap<Pair, WorkerForScheduler>) data[0]);
						
//						sendBackSchedule(wflId,workflowGraph,data,outputStream);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}else if(command.equals("getScheduleForStage")){
					String wflid = inputStream.readUTF();
					List<WorkflowStage> stages = null;
                	HashMap<String, HashMap<Integer,TaskProperties>> stagesTaskProp = null;
                	
                	try {
                		int length = inputStream.readInt();
                		byte[] recvdBytes = new byte[length];
                		inputStream.readFully(recvdBytes);
						stages = (List<WorkflowStage>) programming5.io.Serializer.deserialize(recvdBytes);
						
						length = inputStream.readInt();
						recvdBytes = new byte[length];
						inputStream.readFully(recvdBytes);
						stagesTaskProp = (HashMap<String, HashMap<Integer, TaskProperties>>) programming5.io.Serializer.deserialize(recvdBytes);
						
						HashMap<String,HashMap<Integer,WorkerForScheduler>> scheduleToSend = new HashMap<>(); 
						
						HashMap<Pair, WorkerForScheduler> currentSchedule = this.wfSchedules.get(wflid);
						for(String stage:stagesTaskProp.keySet()){
							
							HashMap<Integer,WorkerForScheduler> taskIDToWorker = new HashMap<>();
							scheduleToSend.put(stage, taskIDToWorker);
							
							HashMap<Integer,TaskProperties> allTasks = stagesTaskProp.get(stage);
							
							for(Integer taskID:allTasks.keySet()){
								TaskProperties task = allTasks.get(taskID);
								
								System.out.println("Task to schedule: " + task);
								for(Pair property:currentSchedule.keySet()){
//									System.out.println(" Comparing task: " + property.getInput() + "  " + property.getOutput());
									if(task.compareIO(property.getInput()) || task.compareIO(property.getOutput())){
										taskIDToWorker.put(task.getTaskId(), currentSchedule.get(property));
										break;
									}
								}
							}
						}
						System.out.println("final schedule to send " + scheduleToSend);
						
						//Now that we have the correct mapping, send back the schedule to the autonomic scheduler.
						byte[] bytesToSend = programming5.io.Serializer.serializeBytes(scheduleToSend);
						outputStream.writeInt(bytesToSend.length);
						outputStream.flush();
						outputStream.write(bytesToSend);
						outputStream.flush();
						
						
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
					
				}
                inputStream.close();
                client.close();
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private Object[] process(WeightedGraph<TaskProperties,DefaultWeightedEdge> workflowGraph, HashMap<String,List<WorkerForScheduler>> allAvailableSlots){
		//TODO: Write the scheduler.
		HashMap<Pair,WorkerForScheduler> taskToWorkers = new HashMap<>();
		/*WorkerForScheduler singleWorker = null;
		for(String key : allAvailableSlots.keySet()){
			singleWorker = allAvailableSlots.get(key).get(0);
			break;
		}*/
		List<String> keySet = new ArrayList<>(allAvailableSlots.keySet());
		Random random = new Random();
		for(DefaultWeightedEdge edge:workflowGraph.edgeSet()){
			TaskProperties tsource=workflowGraph.getEdgeSource(edge);
			TaskProperties ttarget=workflowGraph.getEdgeTarget(edge);
			
			//Getting a random worker for now.
			List<WorkerForScheduler> workerlist = allAvailableSlots.get(keySet.get(random.nextInt(keySet.size())));
			WorkerForScheduler worker = workerlist.get(random.nextInt(workerlist.size()));
			taskToWorkers.put(new Pair(tsource,ttarget), worker);
		}
//		System.out.println("Sent taskToWorkers"+taskToWorkers);
		
		return new Object[]{taskToWorkers};
	}
	
	private boolean sendBackSchedule(String wfID, WeightedGraph<TaskProperties,DefaultWeightedEdge> schedule, Object[] data, DataOutputStream outputStream){/*
		try {
			Socket socket = new Socket(this.serverHost,this.serverPort);
//			DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
//			byte[] scheduledBytes = programming5.io.Serializer.serializeBytes(schedule);
//			outputStream.writeUTF("newSchedule");
//			outputStream.writeUTF(wfID);
			outputStream.writeInt(scheduledBytes.length);
			
			outputStream.flush();
			outputStream.write(scheduledBytes);
			outputStream.flush();
			
			byte[] worketTotaskBytes = programming5.io.Serializer.serializeBytes(data[0]);
			outputStream.writeInt(worketTotaskBytes.length);
			
			outputStream.flush();
			outputStream.write(worketTotaskBytes);
			outputStream.flush();
			
			outputStream.close();
//			socket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return true;
	}
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	public static void main(String[] args) {
		new Thread(new ExternalScheduler(9123, "localhost")).start();
	}
	
	class Pair{
		private TaskProperties input;
		private TaskProperties output;
		public TaskProperties getInput() {
			return input;
		}
		public void setInput(TaskProperties input) {
			this.input = input;
		}
		public TaskProperties getOutput() {
			return output;
		}
		public void setOutput(TaskProperties output) {
			this.output = output;
		}
		public Pair(TaskProperties input, TaskProperties output) {
			super();
			this.input = input;
			this.output = output;
		}
		@Override
		public String toString() {
			return input.toString();
		}
	}
}
