/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************/
package org.eclipse.corrosion.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.test.actions.OpenEditorAtLineAction;
import org.eclipse.corrosion.test.actions.OpenTestAction;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IViewPart;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRoot;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

public class CargoTestViewSupport implements ITestViewSupport {

	public static final String FRAME_PREFIX = " at "; //$NON-NLS-1$

	@Override
	public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
		return new CargoTestRunnerClient(session);
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestCaseElement testCase) {
		return new OpenTestAction(testRunnerPart, testCase);
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestSuiteElement testSuite) {
		return new OpenTestAction(testRunnerPart, testSuite);
	}

	@Override
	public IAction createOpenEditorAction(IViewPart testRunnerPart, ITestElement failure, String traceLine) {
		try {
			int indexOfFramePrefix = traceLine.indexOf(FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			int columNumberIndex = traceLine.lastIndexOf(':');
			int lineNumberIndex = traceLine.lastIndexOf(':', columNumberIndex - 1);
			String testName = traceLine
					.substring(traceLine.indexOf(FRAME_PREFIX) + FRAME_PREFIX.length(), lineNumberIndex).trim();

			String lineNumber = traceLine.substring(lineNumberIndex + 1, columNumberIndex).trim();
			int line = Integer.parseInt(lineNumber);
			return new OpenEditorAtLineAction(testRunnerPart, testName, failure.getTestRunSession(), line);
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}

	@Override
	public Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest) {
		return null;
	}

	@Override
	public String getDisplayName() {
		return "Cargo"; //$NON-NLS-1$
	}

	@Override
	public Collection<StringMatcher> getTraceExclusionFilterPatterns() {
		return Collections.emptySet();
	}

	@Override
	public ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements) {
		if (testElements.isEmpty()) {
			return null;
		}
		ILaunchConfiguration origin = testElements.get(0).getTestRunSession().getLaunch().getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy res;
		try {
			res = origin.copy(origin.getName() + "\uD83D\uDD03"); //$NON-NLS-1$

			ArrayList<String> list = (ArrayList<String>) testElements.stream().map(CargoTestViewSupport::packTestPaths)
					.collect(Collectors.toList());

			// Join path parts into the only string
			StringBuilder sb = new StringBuilder();
			boolean needDelimiter = false;
			for (String v : list) {
				if (needDelimiter) {
					sb.append(' ');
				} else {
					needDelimiter = true;
				}
				sb.append(v.trim());
			}
			res.setAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, sb.toString());
			return res;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
			return null;
		}
	}

	/**
	 * Pack the paths to specified test items to string list.
	 *
	 * @param testElement test element to pack
	 *
	 * @return string list
	 */
	private static String packTestPaths(ITestElement testElement) {
		if (testElement instanceof ITestCaseElement) {
			return testElement.getTestName();
		} else if (testElement instanceof ITestSuiteElement) {
			if (!(testElement.getParent() instanceof ITestRoot)
					&& !(testElement.getParent().getParent() instanceof ITestRoot)) {
				return testElement.getTestName();
			}
		}
		return ""; //$NON-NLS-1$ // Re-Run everything
	}
}
