package com.grepbugs.plugin.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.Validate;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
/**
 * JSON processor for grep rules
 *
 */
public class RulesJSONProcessor {
	public static RulesJSONProcessor INSTANCE = new RulesJSONProcessor();
	private RulesJSONProcessor(){
	}
	
	/**
	 * Parses rules from json file
	 * @param jsonFile {@link File} json file. never pass <code>null</code>
	 * @return {@link Collection} of {@link Rule}s. could b empty.
	 * @throws FileNotFoundException throws when file not found
	 */
	public Collection<Rule> parseRules(File jsonFile) throws FileNotFoundException {
		Validate.notNull(jsonFile, "file should not be null");
		Gson gson = new Gson();
		Reader in;
		in = new FileReader(jsonFile);
		JsonReader reader = new JsonReader(in);
		Rule[] rules = gson.fromJson(reader, Rule[].class);
		return Arrays.asList(rules);
	}
	
	
	/**
	 * Loads saved results from file location
	 * @param fileName
	 * @return
	 * @throws FileNotFoundException
	 */
	public ScanResult fetchResults(String fileName) throws FileNotFoundException{
		Validate.notNull(fileName, "file should not be null");
		Gson gson = new Gson();
		Reader in;
		in = new FileReader(fileName);
		JsonReader reader = new JsonReader(in);
		ScanResult result= gson.fromJson(reader, ScanResult.class);
		return result;
	}
	
	/**
	 * Stores search results into {@link File}
	 * @param searchResult
	 * @param file
	 * @throws IOException
	 */
	public void storeResults(ScanResult searchResult, String fileName) throws IOException{
		Validate.notNull(fileName, " file to store search result should not be null");
		File file = new File(fileName);
		Gson gson = new Gson();
		String jsonString = gson.toJson(searchResult);
		if(file.exists()){
			file.delete();
			file.createNewFile();
		}
		
			ByteArrayInputStream in = null;
			FileOutputStream fout = null;
			try {
				in = new ByteArrayInputStream(jsonString.getBytes());
				fout = new FileOutputStream(file);
				byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1) {
					fout.write(data, 0, count);
				}
			} finally {
				if (in != null)
					in.close();
				if (fout != null)
					fout.close();
			}
		
	}
	
}
