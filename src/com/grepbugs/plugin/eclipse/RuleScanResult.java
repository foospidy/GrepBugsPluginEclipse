package com.grepbugs.plugin.eclipse;

import java.util.Collection;
/**
 * VO that holds {@link Rule} and scanning results as collection of {@link SearchFileDescriptor}s
 */
public class RuleScanResult {

	private Rule rule;
	
	private Collection<SearchFileDescriptor> descriptors;
	
	public Rule getRule() {
		return rule;
	}
	
	public Collection<SearchFileDescriptor> getDescriptors() {
		return descriptors;
	}
	public void setRule(Rule rule) {
		this.rule = rule;
	}
	
	public void setDescriptors(Collection<SearchFileDescriptor> descriptors) {
		this.descriptors = descriptors;
	}
}
