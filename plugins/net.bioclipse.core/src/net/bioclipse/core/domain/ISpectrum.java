/* *****************************************************************************
 * Copyright (c) 2008-2009 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org—epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 * 
 * Contributors:
 *     Stefan Kuhn
 *     
 ******************************************************************************/
package net.bioclipse.core.domain;

import net.bioclipse.core.business.BioclipseException;

/**
 * Object to contains 1D spectra, like 1D NMR spectra, 
 * and mass spectral data.
 *
 * @author egonw
 */
public interface ISpectrum extends IBioObject{

	    /**
	     * Returns an Chemical Markup Language serialization of this
	     * {@link ISpectrum} object.
	     *
	     * @return the {@link ISpectrum} serialized to CML
	     * @throws BioclipseException if CML cannot be returned
	     */
	    public String getCML() throws BioclipseException;
	    
}
