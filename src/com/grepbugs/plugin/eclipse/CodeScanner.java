package com.grepbugs.plugin.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.Policy;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
/**
 * <pre>
 * <b>CORE SCANNING LOGIC</b>
 * Scans selected projects, folders and files with rules that are
 * downloaded from grepbugs.com
 * If it fails to download latest rules, cached rules are used
 * </pre>
 */
public class CodeScanner {

	public Map<Rule, Collection<SearchFileDescriptor>> scan(Collection<IProject> projects, Collection<String> messages) throws Exception{
		Map<Rule, Collection<SearchFileDescriptor>> resultsMap = Maps.newConcurrentMap();
		URLRulesProvider provider = new URLRulesProvider();
		
		/**
		 * Store all rules and scan results in bundle state location
		 */
		IPath path = Activator.getDefault().getStateLocation();
		
		String folderLoc = path.toOSString();
		
		String jsonFileLoc = folderLoc;
		
		Collection<Rule> rules = provider.fetchRules(jsonFileLoc, messages);
		if(rules == null){
			// there must be errors
			return null;
		}
		if(rules.isEmpty())
			return resultsMap;
		 
		 /**
		  * Initiate job for each project
		  */
		 Collection<ProjectScanJob> jobs = Lists.newArrayList();
		 for(IProject project: projects){
			 if(!project.isAccessible()){
				 continue;
			 }
				ProjectScanJob job = new ProjectScanJob(project,rules);
				jobs.add(job);
				job.schedule();
		 }
		try {
			/**
			 * wait for all project jobs to complete.
			 */
			Job.getJobManager().join(PROJECT_SCAN_JOB, new NullProgressMonitor());
		} catch (Exception e) {
			Policy.logException(e);
			throw e;
		}finally{
			Map<Rule, Collection<SearchFileDescriptor>> collectedResultsMap = Maps.newConcurrentMap();
			for(ProjectScanJob job: jobs){
				Map<Rule, Collection<SearchFileDescriptor>> results = job.getResultsMap();
				for(Entry<Rule, Collection<SearchFileDescriptor>> e: results.entrySet()){
					 Collection<SearchFileDescriptor> collectedResults = collectedResultsMap.get(e.getKey());
					 if(collectedResults == null){
						 collectedResults = Sets.newHashSet();
						 collectedResultsMap.put(e.getKey(), collectedResults);
					 }
					 collectedResults.addAll(e.getValue());
				} 
			}
			
			//ignore empty results
			for(Entry<Rule, Collection<SearchFileDescriptor>> e: collectedResultsMap.entrySet()){
				Collection<SearchFileDescriptor> resultsByRule = e.getValue();
				if(resultsByRule == null || resultsByRule.isEmpty()){
					continue;
				}
				resultsMap.put(e.getKey(), resultsByRule);
			}
		}
		return resultsMap;
		
	}
	
	
	
	
	
	private static final Object PROJECT_SCAN_JOB = new Object();
	/**
	 * Job used for scanning set of rules on {@link IProject}
	 */
	private static class ProjectScanJob extends Job{
		private IProject project;
		private Collection<Rule> rules;
		private Map<Rule, Collection<SearchFileDescriptor>> resultsMap;
		public ProjectScanJob(IProject project,Collection<Rule> rules) {
			super("Grepbugs:Scanning Project "+project.getName());
			this.project = project;
			this.rules = rules;
			this.resultsMap =  Maps.newHashMap();
		}
		
		public Map<Rule, Collection<SearchFileDescriptor>> getResultsMap() {
			return resultsMap;
		}
	
		
		@Override
		public boolean belongsTo(Object family) {
			return PROJECT_SCAN_JOB ==  family;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			List<IResource> children = new ArrayList<IResource>();
			collectAllChildren(project, children);
			for(IResource resource: children){
				if(resource.getType() == IResource.FILE){
					for(Rule rule: rules){
						String extn = rule.getExtension();
						String[] extns = StringUtils.split(extn,',');
						Set<String> extensions =  new HashSet<String>(Arrays.asList(extns));
						String fileExtn = resource.getFileExtension();
						if(extensions.contains(StringUtils.lowerCase(fileExtn))){
							File file = resource.getLocation().toFile();
							File projectFile = project.getLocation().toFile();
							Collection<SearchFileDescriptor> results = Sets.newHashSet();
							findMatches(projectFile, file, rule, results);
							Collection<SearchFileDescriptor> collectedResults = resultsMap.get(rule);
							if(collectedResults != null){
								collectedResults.addAll(results);
							}else{
								collectedResults = results;
							}
							resultsMap.put(rule, collectedResults);
						}
					}
				}
			}
			
			return Status.OK_STATUS;
		}
		
		
		private void findMatches(File projectFolder, File file, Rule rule, Collection<SearchFileDescriptor> results){
			
			final String regex = rule.getRegex();
			
			final Pattern pattern = Pattern.compile(regex);
			
			
			List<String> allLines;
			try {
				allLines = Files.readLines(file, Charsets.UTF_8,
						  new LineProcessor<List<String>>() {
						    List<String> result = Lists.newArrayList();
						    public boolean processLine(String line) {
						      result.add(line);
						      return true;
						    }
						    public List<String> getResult() {return result;}
						  });
				int lineNum = 0;
				for(String line: allLines){
					Matcher matcher = pattern.matcher(line);
					while(matcher.find()){
						int start = matcher.start();
						int end = matcher.end();
						if(start == end)
							continue;
						SearchFileDescriptor descriptor = new SearchFileDescriptor();
						descriptor.setLine(lineNum+1);//index from 1
						String projectName = projectFolder.getName();
						String filePath = file.getAbsolutePath();
						String relativePath = StringUtils.substringAfter(filePath, projectFolder.getAbsolutePath());
						String displayName = projectName + relativePath;
						descriptor.setDisplayName(displayName);
						descriptor.setPath(relativePath);
						descriptor.setProjectName(projectName);
						descriptor.setStart(start);
						descriptor.setEnd(end);
						results.add(descriptor);
					}
					lineNum++;
				}
			} catch (IOException e) {
				Policy.logException(e);
			}
		}
		
		
		/**
		 * This method adds all members of the given resource
		 */
		private List<IResource> collectAllChildren(IResource resource, List<IResource> children) {
			if (resource.getType() == IResource.FILE)
				return children;
			IResource[] members;
			try {
				members = ((IContainer) resource).members();
			} catch (CoreException e) {
				//some exception: should we log?
				return children;
			}
			for (IResource m:members) {
				if (IResource.FILE == m.getType()){
					children.add(m);
				}
				else{
					collectAllChildren(m, children);
				}
			}
			return children;
		}
		
	} 
	
	
	
}
