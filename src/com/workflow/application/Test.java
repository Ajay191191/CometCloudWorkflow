package com.workflow.application;

import java.util.ArrayList;
import java.util.List;

public class Test {

	public static void main(String args[]) {
		/**
		 * List<String> contigsListCommand = Util.getContigsListCommand(inputFiles.get(0));
		contigsListCommand.add(workingDir + File.separator + outputContigsFile);
		
//		Util.runProcessWithListOfCommands(contigsListCommand);
		Util.writeShAndStartProcess(contigsListCommand, workingDir, random, "_getContigs.sh");
		Util.splitBAMbyChromosome(new File(workingDir + File.separator + outputContigsFile), inputFiles.get(0));
		 */
		/*System.setProperty("WorkingDir", "/cac/u01/jz362/cometcloud/test/");
		List<String> contigsListCommand = Util.getContigsListCommand(args[0]);
		contigsListCommand.add("/cac/u01/jz362/cometcloud/test/output.contig");
		
//		Util.runProcessWithListOfCommands(contigsListCommand);
		Util.writeShAndStartProcess(contigsListCommand, "/cac/u01/jz362/cometcloud/test/", Math.random(), "_getContigs.sh");
		
		Util.splitBAMbyChromosome(new File("/cac/u01/jz362/cometcloud/test/output.contig"), args[0]);*/
		
		/*final String address=args[0];
		final int port=Integer.parseInt(args[1]);
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Object [] streams=new Object[2];
				DataOutputStream out = null;
				DataInputStream in = null;
				
				try {
					System.out.println("Before socket");
					Socket clientSocket = new Socket(address, port);
					System.out.println("After socket");
					System.out.println("Before outputStream");
					out = new DataOutputStream(clientSocket.getOutputStream());
					System.out.println("After outputStream");					
					in = new DataInputStream(clientSocket.getInputStream());
					System.out.println("After inputStream");
					String agentStatus=in.readUTF();
					System.out.println("After readUTF");
					System.out.println("Status " + agentStatus);
				} catch (UnknownHostException ex) {
					Logger.getLogger(Test.class.getName()).log(Level.SEVERE, "ERROR contacting "+address+":"+port, ex.getMessage());
					ex.printStackTrace();
				} catch (IOException ex) {
					Logger.getLogger(Test.class.getName()).log(Level.SEVERE, "ERROR contacting "+address+":"+port, ex.getMessage());
					ex.printStackTrace();
				}
			}
		}).run();
		*/
		System.out.println("Version 22");
		/*Util.initFilePaths("", "samtools", "", "", "");
		System.setProperty("WorkingDir", "/gpfs/scratch/ajaysudh/output/");
		List<String> contigsListCommand = Util.getContigsListCommand(args[0]);
		contigsListCommand.add("contigs.txt");
		Util.writeShAndStartProcess(contigsListCommand, "/gpfs/scratch/ajaysudh/output/", 1, "_getContigs.sh");
		Util.splitBAMbyChromosome(new File("/gpfs/scratch/ajaysudh/output/contigs.txt"), args[0]);*/
		 
/*		List<String> list = new ArrayList<>();
		list.add("asd");
		list.add("asd");
		list.add("asd");
		list.add("asd");
		list.add("asd");
		list.add("pqr");
		list.add("pqr");
		list.add("pqr");
		list.add("pqr");
		System.out.println(list);
		list.removeAll(Collections.singleton("asd"));
		System.out.println(list);*/
		
		
		int [][]curr = new int[100][100];
		for(int i=1;i<100;i++){
			for(int j=1;j<100;j++){
				curr[i][j]=1;
			}
		}
		List<String>  list = new ArrayList<>();
//		List<Integer>  list = new ArrayList<>();
	}
	
	
}
