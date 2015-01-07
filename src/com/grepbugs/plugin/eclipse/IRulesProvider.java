package com.grepbugs.plugin.eclipse;

import java.io.IOException;
import java.util.Collection;

/**
 * 
 * Provides interface methods to load grep rules
 */
public interface IRulesProvider {

	/**
	 * Fetches rules from source
	 * @return {@link Collection} of {@link Rule}s. never <code>null</code>. could be empty.
	 * @throws IOException 
	 */
	public Collection<Rule> fetchRules(String fileToStore, Collection<String> messages) throws Exception;
}
