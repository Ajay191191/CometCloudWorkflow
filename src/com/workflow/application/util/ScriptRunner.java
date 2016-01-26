package com.workflow.application.util;

import java.io.File;

public class ScriptRunner{
	public static void main(String[] args) {
		String workingDir = args[0];
		String shFile = args[1];
		Util.runScript(workingDir+File.separator+shFile);
	}
}