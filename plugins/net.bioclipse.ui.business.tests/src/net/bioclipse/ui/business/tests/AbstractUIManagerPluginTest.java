/* *****************************************************************************
 * Copyright (c) 2008  Egon Willighagen <egonw@users.sf.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.ui.business.tests;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.ui.business.IUIManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractUIManagerPluginTest {

    protected static IUIManager ui;
    
    @Test
    public void testManagerInstantiation() {
    	// the instance is created by the subclass prior to this test
    	Assert.assertNotNull(ui);
    }
    
    @Ignore
    @Test
    public void testOpenListIFile() throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject("TEST");
        project.create(new NullProgressMonitor());
        project.open(new NullProgressMonitor());
        String filePath = "/TEST/testFile99883423427.txt";
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
                         new Path(filePath)
                     );
        ui.save( file, 
                 new ByteArrayInputStream("test file".getBytes()), 
                 null );
        final IFile savedFile = ResourcesPlugin.getWorkspace()
                                               .getRoot()
                                               .getFile( new Path(filePath) );
        Assert.assertTrue(savedFile.exists());
        ui.openFiles( new ArrayList<IFile>() {{add(savedFile);}});
        project.delete(true, new NullProgressMonitor());
    }
    
    @Ignore
    public void testOpen_String() {
        String filePath = "/Virtual/testFile99883423426.txt";
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        ui.save(
            file, new ByteArrayInputStream("test file".getBytes()), null
        );
        IFile savedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertTrue(savedFile.exists());
        ui.open(filePath);
    }

    @Ignore
    public void testSaveAndRemove_IFile() {
        String filePath = "/Virtual/testFile683442689.txt";
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        ui.save(
            file, new ByteArrayInputStream("test file".getBytes()),
            null
        );
        IFile savedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertTrue(savedFile.exists());
        ui.remove(savedFile);
        IFile removedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertFalse(removedFile.exists());
    }

    @Ignore
    public void testSaveAndRemove_String() {
        String filePath = "/Virtual/testFile124879043.txt";
        ui.save(
            filePath, new ByteArrayInputStream("test file".getBytes())
        );
        IFile savedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertTrue(savedFile.exists());
        ui.remove(filePath);
        IFile removedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertFalse(removedFile.exists());
    }

    @Ignore
    public void testExists_String() {
        String filePath = "/Virtual/testFile887434232.txt";
        ui.save(
            filePath, new ByteArrayInputStream("test file".getBytes())
        );
        Assert.assertTrue(ui.fileExists(filePath));
        ui.remove(filePath);
        Assert.assertFalse(ui.fileExists(filePath));
    }

    @Ignore
    public void testExists_IFile() {
        String filePath = "/Virtual/testFile734248911.txt";
        ui.save(
            filePath, new ByteArrayInputStream("test file".getBytes())
        );
        IFile savedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertTrue(ui.fileExists(savedFile));
        ui.remove(filePath);
        IFile removedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertFalse(ui.fileExists(removedFile));
    }

    @Ignore
    public void testReadFileIntoArray_IFile() throws BioclipseException {
        String filePath = "/Virtual/testFile734248911.txt";
        ui.save(
            filePath, new ByteArrayInputStream("test file".getBytes())
        );
        IFile savedFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
            new Path(filePath)
        );
        Assert.assertTrue(ui.fileExists(savedFile));
        String[] contents = ui.readFileIntoArray(savedFile);
        Assert.assertNotNull(contents);
        Assert.assertEquals(1, contents.length);
        Assert.assertEquals("test file", contents[0]);
    }
}
