package com.workflow.application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.workflow.application.util.Util;

import tassl.application.utils.CommonUtils;

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
		
		final String address=args[0];
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
		
		
		
	}

}
