package com.workflow.application.tasks.worker;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import com.workflow.application.util.Util;

public class ContigSplitBAMWorker implements Worker<String>{

	private String chr;
	private String inputBAM;
	private String outputBAM;

	public ContigSplitBAMWorker(String chr, String inputBAM) {
		super();
		this.chr = chr;
		this.inputBAM = inputBAM;
		this.outputBAM = inputBAM.replace(".bam", "")+"_"+chr+".bam";
	}

	public String getInputBAM() {
		return inputBAM;
	}

	public void setInputBAM(String inputBAM) {
		this.inputBAM = inputBAM;
	}

	public String getChr() {
		return chr;
	}

	public void setChr(String chr) {
		this.chr = chr;
	}

	@Override
	public String call() throws Exception {
		List<String> splitBAMByChromosomeCommand = Util.getSplitBAMByChromosomeCommand(chr, inputBAM, outputBAM);
//		Util.runProcessWithListOfCommands(splitBAMByChromosomeCommand);
		Util.writeShAndStartProcess(splitBAMByChromosomeCommand, System.getProperty("WorkingDir"), Math.random()*1000, "_splitBAM.sh");
		
		return new File(outputBAM).getName();
	}
	
}