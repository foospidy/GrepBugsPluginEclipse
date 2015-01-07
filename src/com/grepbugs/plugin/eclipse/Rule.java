package com.grepbugs.plugin.eclipse;


/**
 * 
 * Rule: vo that holds rule information
 *
 */
public class Rule {
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}

	public String getExtension() {
		return extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	public String getRegex() {
		return regex;
	}
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setTags(String tags) {
		this.tags = tags;
	}
	public String getTags() {
		return tags;
	}
	
	public Rule() {
	}


	private String id;
	private String language;
	private String extension;
	private String regex;
	private String description;
	private String tags;
}
