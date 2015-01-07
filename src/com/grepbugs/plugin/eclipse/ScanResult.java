package com.grepbugs.plugin.eclipse;

import java.util.Collection;
import java.util.Date;

/**
 * Search Results by ruleId
 * This class is used for storing last search results
 *
 */
public class ScanResult {
	
	private Date searchDate;
	private Collection<RuleScanResult> ruleResults;
	public Date getSearchDate() {
		return searchDate;
	}
	public void setSearchDate(Date searchDate) {
		this.searchDate = searchDate;
	}
	public Collection<RuleScanResult> getRuleResults() {
		return ruleResults;
	}
	public void setRuleResults(Collection<RuleScanResult> ruleResults) {
		this.ruleResults = ruleResults;
	}
}
