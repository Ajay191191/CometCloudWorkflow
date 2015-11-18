package com.workflow.application;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.io.IOUtils;

public class Test {

	public static void main(String args[]) {
		try {
			sendPost();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// HTTP POST request
	private static void sendPost() throws Exception {

		String url = "https://api.projectoxford.ai/emotion/v1.0/recognize";
		URL obj = new URL(url);
		System.out.println(new URL("http://nothippyjusthealthy.com/wp-content/uploads/2014/06/Curtis-family-090-re.jpg").toString());
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		byte[] extractBytes = extractBytes("C:/Users/Ajay/Downloads/Curtis-family-090-re.png");
		con.setRequestMethod("POST");
		con.setRequestMethod("POST");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setRequestProperty("Ocp-Apim-Subscription-Key", "b4f6718ebe2145a1b53196824296aff3");
		con.setRequestProperty("Content-Type", "application/octet-stream");
		con.setRequestProperty("Content-Length", extractBytes.length+"");
//		String urlParameters = "{\"url\":\"http://nothippyjusthealthy.com/wp-content/uploads/2014/06/Curtis-family-090-re.jpg\"}";

        OutputStream os = con.getOutputStream();
        DataOutputStream writer = new DataOutputStream(os);
		writer.write(extractBytes);
        writer.flush();
        writer.close();
        os.close();
		
		
        System.out.println(con.toString());

		int responseCode = con.getResponseCode();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		System.out.println(response.toString());

	}
	
	public static byte[] extractBytes (String ImageName) {
		try {
			InputStream image = new FileInputStream(ImageName);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			int bytesRead;
			byte[] bytes = new byte[1024];
			while ((bytesRead = image.read(bytes)) > 0) {
			    byteArrayOutputStream.write(bytes, 0, bytesRead);
			}
			byte[] data = byteArrayOutputStream.toByteArray();
			return data;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
