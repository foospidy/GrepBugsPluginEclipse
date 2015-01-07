package com.grepbugs.plugin.eclipse.ui;

import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
/**
 * Handler to run grep bugs command
 */
public class RunGrepBugsHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if(selection.isEmpty())
			return null;
		Collection<IProject> projects = SelectionUtils.getProjects(selection);
		if(!projects.isEmpty()){
			try {
				CodeScannerView view = (CodeScannerView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(CodeScannerView.ID);
				if(view != null){
					view.scan(projects);
				}
			} catch (PartInitException e) {
			   Policy.logException(e);
			}
		}
		return null;
		
	}
	
	@Override
	public void setEnabled(Object evaluationContext) {
		super.setEnabled(evaluationContext);
	}

}
