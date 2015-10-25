

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Test {
	public static void main(String[] args) {
		try {
			List<String> command = new ArrayList<String>();
//			command.add("./print");
			command.add("/cac/u01/jz362/Workflow/bwa/bwa-0.7.12/bwa");
			command.add("mem");
			command.add("-M");
			command.add("-t");
			command.add("20");
			command.add("-R \"@RG\\tID:group1\\tSM:SRR622457\\tPL:illumina\\tLB:lib1\\tPU:unit1\"");
			command.add("/cac/u01/jz362/Workflow/Reference/hg19.fasta");
			command.add("/cac/u01/jz362/cometcloud/tmp/SRR017279_1_aa");
			command.add("/cac/u01/jz362/cometcloud/tmp/SRR017279_2_aa");
			command.add(">");
			command.add("SRR017279_2_aa11");
			String[] array = command.toArray(new String[0]);
			ProcessBuilder pb = new ProcessBuilder(array);
			for(String arr: array){
				System.out.println("command " + arr);
			}
			System.out.println("command " + pb.command());
			Process process = pb.start();
			
			final InputStream errorStream = process.getErrorStream();
			new Runnable() {
				
				@Override
				public void run() {
					extracted(errorStream);
				}
			}.run();
			final InputStream inputStream = process.getInputStream();
			new Runnable(){

				@Override
				public void run() {
					
					extracted(inputStream);
				}
				
			}.run();
			int waitFor = process.waitFor();
			System.out.println("Wait for " + waitFor);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	private static void extracted(InputStream inputStream) {
		try {
			BufferedReader reader =new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ( (line = reader.readLine()) != null) {
				System.out.println("Stream "+ line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
