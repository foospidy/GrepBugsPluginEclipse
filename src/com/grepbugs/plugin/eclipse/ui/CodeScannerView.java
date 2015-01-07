package com.grepbugs.plugin.eclipse.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Policy;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.grepbugs.plugin.eclipse.Activator;
import com.grepbugs.plugin.eclipse.CodeScanner;
import com.grepbugs.plugin.eclipse.RulesJSONProcessor;
import com.grepbugs.plugin.eclipse.Rule;
import com.grepbugs.plugin.eclipse.RuleScanResult;
import com.grepbugs.plugin.eclipse.ScanResult;
import com.grepbugs.plugin.eclipse.SearchFileDescriptor;

/**
 * CodeScannerView: View part to show GrepBugs Results
 * 
 */
public class CodeScannerView extends ViewPart {

	private static final String PROJECT_CLOSED = "Project Closed";
	private static final String GREPRESULTS_FILE = "grepresults.json";
	public static final String ID = "com.grepbugs.codescanner.view";
	private TableViewer tagsViewer;
	private TableViewer matchResultsViewer;
	private Map<Rule, Collection<SearchFileDescriptor>> results = Maps
			.newHashMap();
	private Label loadLbl;
	private Link noResultsLbl;

	@Override
	public void createPartControl(final Composite parent) {

		SashForm base = new SashForm(parent, SWT.HORIZONTAL);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(2, 2)
				.applyTo(base);
		Composite leftComp = new Composite(base, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(2, 2)
				.applyTo(leftComp);
		GridData data = new GridData(GridData.FILL_BOTH);
		leftComp.setLayoutData(data);

		loadLbl = new Label(leftComp, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		loadLbl.setLayoutData(data);
		loadLbl.setFont(SWTResourceManager.getBoldFont(loadLbl.getFont()));

		
		
		Composite tagsComp = new Composite(leftComp, SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		tagsComp.setLayoutData(data);
		StackLayout stackLayout = new StackLayout();
		tagsComp.setLayout(stackLayout);
		
		//results label
		noResultsLbl = new Link(tagsComp, SWT.WRAP);
		noResultsLbl.setText(Messages.CodeScannerView_NO_BUGS_FOUND);
		Control resultsControl = buildTagsViewer(tagsComp);
		stackLayout.topControl = resultsControl;
		
		final CTabFolder folder = new CTabFolder(base, SWT.BORDER);
		Color selectionBgColor = folder.getDisplay().getSystemColor(
				SWT.COLOR_GRAY);
		folder.setSelectionBackground(selectionBgColor);
		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.CodeScannerView_MATCHES);

		Control matchResultControl = buildMatchResultsViewer(folder);
		item.setControl(matchResultControl);

		item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.CodeScannerView_DESCRITION);
		Composite descComp = new Composite(folder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(2, 2)
		.applyTo(descComp);
		final StyledText descriptinLbl = new StyledText(descComp, SWT.WRAP
				| SWT.MULTI | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_BOTH);
		descriptinLbl.setLayoutData(data);
		item.setControl(descComp);

		item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.CodeScannerView_REGEX);
		Composite regExComp = new Composite(folder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(2, 2)
		.applyTo(regExComp);
		final StyledText regExLbl = new StyledText(regExComp, SWT.WRAP | SWT.MULTI
				| SWT.READ_ONLY);
		item.setControl(regExComp);
		data = new GridData(GridData.FILL_BOTH);
		regExLbl.setLayoutData(data);
		
		
		
		item = new CTabItem(folder, SWT.NONE);
		item.setText(Messages.CodeScannerView_ABOUT);
		Composite aboutComp = new Composite(folder, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(2, 2)
		.applyTo(aboutComp);
		final Link aboutLbl = new Link(aboutComp, SWT.WRAP | SWT.MULTI
				| SWT.READ_ONLY);
		aboutLbl.setText(Messages.CodeScannerView_ABOUT_TEXT);
		item.setControl(aboutComp);
		data = new GridData(GridData.FILL_BOTH);
		aboutLbl.setLayoutData(data);
		
		
		
		
		data = new GridData(GridData.FILL_BOTH);
		folder.setLayoutData(data);

		tagsViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = event.getSelection();
						Rule rule = null;
						if (!selection.isEmpty()) {
							rule = (Rule) ((IStructuredSelection) selection)
									.getFirstElement();
						}

						Collection<SearchFileDescriptor> searchDescriptors = results
								.get(rule);
						if (searchDescriptors == null) {
							searchDescriptors = Sets.newHashSet();
						}

						matchResultsViewer.setInput(searchDescriptors.toArray());

						String description = ""; //$NON-NLS-1$
						if (rule != null) {
							description = rule.getDescription();
						}
						descriptinLbl.setText(description != null ? description
								: ""); //$NON-NLS-1$

						String regEx = ""; //$NON-NLS-1$
						if (rule != null) {
							regEx = rule.getRegex();
						}
						regExLbl.setText(regEx != null ? regEx : ""); //$NON-NLS-1$

						if (folder.getSelectionIndex() == -1) {
							folder.setSelection(0);
						}
					}
				});

		matchResultsViewer.addOpenListener(new IOpenListener() {

			@Override
			public void open(OpenEvent event) {
				ISelection selection = event.getSelection();
				SearchFileDescriptor searchFileDescriptor = null;
				if (!selection.isEmpty()) {
					searchFileDescriptor = (SearchFileDescriptor) ((IStructuredSelection) selection)
							.getFirstElement();
				}
				if (searchFileDescriptor != null) {
					String path = searchFileDescriptor.getPath();
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IWorkspaceRoot root = workspace.getRoot();
					String projectName = searchFileDescriptor
							.getProjectName();
					IProject project = root.getProject(projectName);
					if (project != null) {
						if (project.isAccessible()) {
							IFile file = project.getFile(path);
							IWorkbenchPage page = PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow().getActivePage();
							try {
								IEditorPart editorPart = IDE.openEditor(page,
										file);
								if (editorPart instanceof ITextEditor) {

									ITextEditor editor = (ITextEditor) editorPart;

									IDocumentProvider provider = editor
											.getDocumentProvider();
									IDocument document = provider
											.getDocument(editor
													.getEditorInput());
									try {

										int start = document
												.getLineOffset(searchFileDescriptor
														.getLine());
										editor.selectAndReveal(
												Math.max(start - 1, 0), 0);
										IWorkbenchPage editorPage = editor
												.getSite().getPage();
										editorPage.activate(editor);
									} catch (BadLocationException x) {
										// ignore
									}
								}
							} catch (PartInitException e) {
								MessageDialog.openError(getViewSite()
										.getShell(), "Error",
										"Failed to open Grepbugs view");
							}

						} else {
							MessageDialog.openInformation(getViewSite().getShell(),PROJECT_CLOSED , Messages.bind(Messages.CodeScannerView_PROJECT_CLOSED, project.getName()));
						
						}
					} else {
						Policy.getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, "project not found in workspace project:"+projectName));
					}

				}
			}
		});
		
		

		base.setWeights(new int[] { 50, 50 });
		
		//load saved results into view
		loadSavedResults();
		
	}

	private Control buildMatchResultsViewer(Composite parent) {

		matchResultsViewer = new TableViewer(parent, SWT.FULL_SELECTION
				| SWT.SINGLE | SWT.BORDER);

		autoResizeViewer(matchResultsViewer);
		TableViewerColumn column = new TableViewerColumn(matchResultsViewer,
				SWT.NONE);
		column.getColumn().setText(Messages.CodeScannerView_RULE);
		column.getColumn().setWidth(100);
		matchResultsViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SearchFileDescriptor) {
					SearchFileDescriptor result = (SearchFileDescriptor) element;
					return StringUtils.join(new String[] {
							result.getDisplayName(),
							Messages.CodeScannerView_LINE,
							String.valueOf(result.getLine()) });
				}
				return ""; //$NON-NLS-1$
			}
		});
		matchResultsViewer.setContentProvider(new ArrayContentProvider());
		matchResultsViewer.getTable().setHeaderVisible(false);
		matchResultsViewer.getTable().setLinesVisible(false);
		matchResultsViewer.setComparator(new ViewerComparator(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		}));
		return matchResultsViewer.getControl();
	}

	private void autoResizeViewer(final TableViewer viewer) {
		viewer.getTable().addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {

				Table table = viewer.getTable();
				int columnCount = table.getColumnCount();
				if (columnCount == 0)
					return;
				Rectangle area = table.getClientArea();
				int totalAreaWdith = area.width;
				int lineWidth = table.getGridLineWidth();
				int totalGridLineWidth = (columnCount - 1) * lineWidth;
				int totalColumnWidth = 0;
				for (TableColumn column : table.getColumns()) {
					totalColumnWidth = totalColumnWidth + column.getWidth();
				}
				int diff = totalAreaWdith
						- (totalColumnWidth + totalGridLineWidth);

				for (TableColumn column : table.getColumns()) {
					int pixels = (int) (diff * ((column.getWidth() * 1.0f) / totalColumnWidth));
					column.setWidth(column.getWidth() + pixels);
				}
			}
		});
	}

	private Control buildTagsViewer(Composite parent) {

		tagsViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL);

		autoResizeViewer(tagsViewer);

		TableViewerColumn column = new TableViewerColumn(tagsViewer,
				SWT.NONE);
		column.getColumn().setText(Messages.CodeScannerView_RULE_LABEL);
		column.getColumn().setWidth(100);
		tagsViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Rule) {
					Rule rule = (Rule) element;
					Collection<SearchFileDescriptor> searchDescriptors = results
							.get(rule);
					if (searchDescriptors == null) {
						searchDescriptors = Sets.newHashSet();
					}
					return StringUtils.join(new String[] { rule.getTags(),
							" (", String.valueOf(searchDescriptors.size()), ")" }); //$NON-NLS-1$ //$NON-NLS-2$
				}
				return ""; //$NON-NLS-1$
			}
		});
	
		tagsViewer.setContentProvider(new ArrayContentProvider());
		tagsViewer.getTable().setHeaderVisible(false);
		tagsViewer.getTable().setLinesVisible(false);
		tagsViewer.setComparator(new ViewerComparator(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		}));
		return tagsViewer.getControl();
	}

	@Override
	public void setFocus() {

	}

	public void scan(Collection<IProject> projects) {
		scanProjects(projects);
	}
	
	
	private void loadSavedResults() {

		String fileName = getScanResultFileLocation();
		try {
			ScanResult result = RulesJSONProcessor.INSTANCE.fetchResults(fileName);
			results.clear();
			if (result.getRuleResults() != null) {
				for (RuleScanResult ruleResult : result.getRuleResults()) {
					results.put(ruleResult.getRule(),
							ruleResult.getDescriptors());
				}
			}

			tagsViewer.setInput(results.keySet().toArray());
			tagsViewer
					.setSelection(results.isEmpty() ? StructuredSelection.EMPTY
							: new StructuredSelection(results.keySet()
									.iterator().next()));

			loadLbl.setText(Messages.bind(
					Messages.CodeScannerView_SCANRESULTS_TIME,
					DateFormat.getDateTimeInstance().format(
							result.getSearchDate())));

		} catch (FileNotFoundException e) {
			Policy.logException(e);
		}

	}
	

	private void scanProjects(Collection<IProject> projects) {
		
		CodeScanner scanner = new CodeScanner();
		Map<Rule, Collection<SearchFileDescriptor>> recentResults;
		String lblTxt = "";
		try {
			lblTxt = Messages.CodeScannerView_SCAN_LABEL;
			loadLbl.setText(lblTxt); 
			Collection<String> messages = Sets.newHashSet();
			recentResults = scanner.scan(projects, messages);
			
			//update UI with results
			results.clear();
			if(recentResults != null){
				results.putAll(recentResults);
			}
			tagsViewer.setInput(results.keySet().toArray());
			tagsViewer
					.setSelection(results.isEmpty() ? StructuredSelection.EMPTY
							: new StructuredSelection(results.keySet()
									.iterator().next()));
			
			if(recentResults != null && recentResults.isEmpty()){
				showResultsEmptyMsg();
			}else{
				showTagsControl();
			}
			if (!messages.isEmpty()) {
				MessageDialog.openInformation(getViewSite().getShell(),
						Messages.CodeScannerView_INFORMATION,
						StringUtils.join(messages, " ")); //$NON-NLS-2$
			}
			lblTxt = Messages.bind(Messages.CodeScannerView_SCANRESULTS_TIME,
					DateFormat.getDateTimeInstance().format(new Date()));

		} catch (Exception e1) {
			Policy.logException(e1);
			MessageDialog.openError(getViewSite().getShell(),
					Messages.CodeScannerView_ERROR,
					Messages.CodeScannerView_FAILED_DOWNLOAD + e1.getMessage());
			lblTxt = "";
		} finally {
			loadLbl.setText(lblTxt); //$NON-NLS-1$
		}
		
		
		//clean last saved results and store new results
		
		if(results != null){
			ScanResult scanResult = new ScanResult();
			
			List<RuleScanResult> ruleResults = Lists.newArrayList();
			for(Entry<Rule, Collection<SearchFileDescriptor>> e: results.entrySet()){
				RuleScanResult ruleResult = new RuleScanResult();
				ruleResult.setRule(e.getKey());
				ruleResult.setDescriptors(e.getValue());
				ruleResults.add(ruleResult);
			}
			scanResult.setRuleResults(ruleResults);
			scanResult.setSearchDate(new Date());
			String jsonFileName = getScanResultFileLocation();
			try {
				RulesJSONProcessor.INSTANCE.storeResults(scanResult, jsonFileName);
			} catch (IOException e) {
				//ignore do not show this error
				Policy.logException(e);
			}
			
		}
	}

	private void showTagsControl() {
		Composite comp = tagsViewer.getControl().getParent();
		((StackLayout)comp.getLayout()).topControl = tagsViewer.getControl();
		comp.layout(true);
		
	}

	private void showResultsEmptyMsg() {
		Composite comp = tagsViewer.getControl().getParent();
		((StackLayout)comp.getLayout()).topControl = noResultsLbl;
		comp.layout(true);
	}

	private String getScanResultFileLocation() {
		//bundle state location
		IPath path = Activator.getDefault().getStateLocation();
		String folderLoc = path.toOSString();
		String jsonFileName = folderLoc+File.separator+GREPRESULTS_FILE;
		return jsonFileName;
	}

}
