package com.grepbugs.plugin.eclipse.ui;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.google.common.collect.Sets;

/**
 * Utils to grab projects from {@link ISelection}
 */
public class SelectionUtils {

	public static Collection<IProject> getProjects(ISelection selection) {
		Set<IProject> projects = Sets.newHashSet();
		if(selection instanceof IStructuredSelection){
			IStructuredSelection sel = (IStructuredSelection)selection;
			Object[] selectedObjects = sel.toArray();
			//check all objects are instanceof projects or not?
			
			for(Object obj: selectedObjects){
				
				if(obj instanceof IAdaptable){
					IAdaptable adaptable = ((IAdaptable)obj);
					obj = adaptable.getAdapter(IProject.class);
				}
				if(obj instanceof IProject){
					projects.add((IProject) obj);
				}
			}
		}
		return projects;
	}

}
