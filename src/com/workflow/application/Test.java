package com.workflow.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

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
		
		
//		Util.runProcessWithShell(".", args[0]);
		
//		Util.startNewProcessAndRunScript(args[0], args[1]);
		
		/*Object [] streams=new Object[2];
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, "ERROR contacting "+args[0]+":"+args[1], ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, "ERROR contacting "+args[0]+":"+args[1], ex.getMessage());
        }
        streams[0]=in;
        streams[1]=out;*/
		

		try {
			AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials("AKIAJGFR6YNHCHWN5M3Q", "IYxNBCd23pSJlXFWXxCuI3by6ukuEQlDp5inLgPt"));
			S3Object object = s3Client.getObject(new GetObjectRequest("bioreference", "hg19.fasta.amb"));
			String md5Hex = DigestUtils.md5Hex(new FileInputStream(new File("/home/ajay/hg19.fasta.amb")));
			System.out.println(md5Hex);
			System.out.println(object.getObjectMetadata().getETag());
			
			/*InputStream objectData = object.getObjectContent();
			FileOutputStream outputStream = new FileOutputStream(new File("/home/ajay/hg19.fasta.amb"));
			
			
			int read = -1;

			while ( ( read = objectData.read() ) != -1 ) {
				outputStream.write(read);
			}

			outputStream.close();
			// Process the objectData stream.
			objectData.close();
*/			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        
	}
	
	
}
