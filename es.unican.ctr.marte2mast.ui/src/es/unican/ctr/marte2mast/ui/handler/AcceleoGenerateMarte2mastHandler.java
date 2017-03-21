/*******************************************************************************
 * Copyright (c) 2008, 2010, 2014 Obeo, Kroehnert
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Kroehnert - update towards new Eclipse menu mechanism
 *******************************************************************************/
package es.unican.ctr.marte2mast.ui.handler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import es.unican.ctr.marte2mast.ui.Activator;
import es.unican.ctr.marte2mast.ui.common.GenerateAll;
import es.unican.ctr.marte2mast.ui.preferences.Marte2MastPreferenceConstants;

/**
 * Marte2mast code generation.
 */
public class AcceleoGenerateMarte2mastHandler extends AbstractHandler {
	private final String outputDirectory = Activator.getDefault().getPreferenceStore().getString(Marte2MastPreferenceConstants.P_OUTPUT_PATH);

	/**
	 * Create a runnable which generates the mast files from models if @param
	 * files is not empty
	 */
	public void generateMastModel(final List<IFile> files) {
		if (files.isEmpty()) {
			return;
		}
		IRunnableWithProgress operation = createRunnable(files);
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, operation);
		} catch (InvocationTargetException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			Activator.getDefault().getLog().log(status);
		} catch (InterruptedException e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			Activator.getDefault().getLog().log(status);
		}
	}

	/**
	 * Creates a runnable which transforms the selected files into mast models
	 * 
	 * @param files
	 *            List of files to transform
	 * @return parameterized runnable
	 */
	private IRunnableWithProgress createRunnable(final List<IFile> files) {
		IRunnableWithProgress operation = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					Iterator<IFile> filesIt = files.iterator();
					while (filesIt.hasNext()) {
						IFile model = (IFile) filesIt.next();
						URI modelURI = URI.createPlatformResourceURI(model.getFullPath().toString(), true);
						try {
							IContainer target = model.getProject().getFolder(outputDirectory);
							GenerateAll generator = new GenerateAll(modelURI, target.getLocation().toFile(), getArguments());
							generator.doGenerate(monitor);
						} catch (IOException e) {
							IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
							Activator.getDefault().getLog().log(status);
						} finally {
							model.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
						}
					}
				} catch (CoreException e) {
					IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
					Activator.getDefault().getLog().log(status);
				}
			}
		};
		return operation;
	}

	/**
	 * Computes the arguments of the generator.
	 * 
	 * @return the arguments
	 * @generated
	 */
	protected List<? extends Object> getArguments() {
		return new ArrayList<String>();
	}

	/**
	 * This function is called, when the menu item in the popup is selected. It
	 * transforms the selection into actual files and calls the
	 * generateMastModel method
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);

		List<IFile> modelFiles = new ArrayList<IFile>();
		if (selection instanceof IStructuredSelection) {
			List<?> selectionList = ((IStructuredSelection) selection).toList();
			for (Object object : selectionList) {
				if (object instanceof org.eclipse.papyrus.infra.onefile.model.ISubResourceFile) {
					modelFiles.add(((org.eclipse.papyrus.infra.onefile.model.ISubResourceFile) object).getFile());
				}
			}
			generateMastModel(modelFiles);
		} else {
			MessageDialog.openInformation(shell, "Info", "Please select a .uml file");
		}
		return null;
	}

}
