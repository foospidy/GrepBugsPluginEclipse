package com.grepbugs.plugin.eclipse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * Rules provider: provides rules content from {@link URL}
 * 
 */
public class URLRulesProvider implements IRulesProvider {

	private static final String GREPRULES_FILE = "greprules.json";
	private static final String FAILED_TO_CREATE_GREP_RULES_FILE = "Failed to create grep rules file";
	private static final String NO_CACHED_GREP_RULES_EXIST = "No cached grep rules exist";
	private static final String STALE_GREP_RULES_ARE_BEING_USED = "Stale grep rules are being used.";
	private static final String FAILED_TO_DOWNLOAD_GREP_RULES = "Failed to download grep rules.";
	
	
	private String url = "https://grepbugs.com/rules";

	@Override
	public Collection<Rule> fetchRules(String fileToStore,
			Collection<String> messages) throws Exception {
		String jsonFileName = File.separator+GREPRULES_FILE;
		Collection<Rule> rules = new ArrayList<Rule>();
		Path target = Paths.get(fileToStore+jsonFileName);
		
		//delete old file
		File rulesFile = target.toFile();
		boolean oldRulesFileExists = rulesFile.exists();
		File targetRulesFile = null;
		//old file
		if(oldRulesFileExists){
			targetRulesFile = rulesFile;
		}
		/**
		 * load new rules into temporary file and rename temporary file later 
		 * if rules are downloaded successfully.
		 */
		String tempFilePath = fileToStore+File.separator+"temp.json";
		Path tempTarget = Paths.get(tempFilePath);
		
		File jsonFile = tempTarget.toFile();
		boolean copyFile = true;
		boolean fileExists = jsonFile.exists();
		if(fileExists){
			jsonFile.delete();
		}
		
		copyFile = jsonFile.createNewFile();
		
		if (copyFile) {
			try {
				saveFile(tempFilePath, url);
				//Delete existing rules
				rulesFile.delete();
				//rename temp file to rules file
				jsonFile.renameTo(rulesFile);
				targetRulesFile = rulesFile;
			} catch (Exception e) {
				messages.add(FAILED_TO_DOWNLOAD_GREP_RULES);
				if (oldRulesFileExists) {
					messages.add(STALE_GREP_RULES_ARE_BEING_USED);
				}else{
					messages.add(NO_CACHED_GREP_RULES_EXIST);
					rules = null;//no results
				}
				// copy failed use existing file
			} 
		}else{
			messages.add(FAILED_TO_CREATE_GREP_RULES_FILE);
		}
		
		if(targetRulesFile != null){
			rules = RulesJSONProcessor.INSTANCE.parseRules(targetRulesFile);
		}
		return rules;
	}

	private void saveFile(String fileName, String url) throws Exception {
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			URL urlObject = new URL(url);
			URLConnection connection = urlObject.openConnection();
			//add user-agent
			connection.addRequestProperty("User-Agent", "GrepBugs for Eclipse (1.0)");
			in = new BufferedInputStream(connection.getInputStream());
			fout = new FileOutputStream(fileName);
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
