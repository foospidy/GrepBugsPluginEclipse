package com.grepbugs.plugin.eclipse.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.grepbugs.plugin.eclipse.ui.messages"; //$NON-NLS-1$
	public static String CodeScannerView_DESCRITION;
	public static String CodeScannerView_ERROR;
	public static String CodeScannerView_FAILED_DOWNLOAD;
	public static String CodeScannerView_INFORMATION;
	public static String CodeScannerView_LINE;
	public static String CodeScannerView_MATCHES;
	public static String CodeScannerView_REGEX;
	public static String CodeScannerView_ABOUT;
	public static String CodeScannerView_RULE;
	public static String CodeScannerView_RULE_LABEL;
	public static String CodeScannerView_SCAN;
	public static String CodeScannerView_SCAN_LABEL;
	public static String CodeScannerView_SCANRESULTS_TIME;
	public static String CodeScannerView_PROJECT_CLOSED;
	public static String CodeScannerView_NO_BUGS_FOUND;
	public static String CodeScannerView_ABOUT_TEXT;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
