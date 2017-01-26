/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: JarTest.java $
 *
 * IBM Data Engine for NoSQL - Power Systems Edition User Library Project
 *
 * Contributors Listed Below - COPYRIGHT 2015,2016,2017
 * [+] International Business Machines Corp.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * IBM_PROLOG_END_TAG
 */
package com.ibm.research.capiblock.jar;

import java.io.IOException;

import com.ibm.research.capiblock.CapiBlockDevice;
import com.ibm.research.capiblock.Chunk;

/**
 * Small functional test for the Jar distribution of the library
 * 
 * @author Jan S. Rellermeyer, IBM Research
 *
 */
public class JarTest {

	static final String CAPI_DEVICE_PATH_PROP = "CAPI_DEVICE_PATH"; //$NON-NLS-1$ 

	static final String DEVICE = System.getProperty(CAPI_DEVICE_PATH_PROP, "/dev/sdc"); //$NON-NLS-1$

	public static void main(final String[] args) throws IOException {

		final CapiBlockDevice dev = CapiBlockDevice.getInstance();
		try (final Chunk chunk = dev.openChunk(DEVICE)) {
			// nop
		}
		
		System.err.println("### JarTest SUCCESS ###"); //$NON-NLS-1$
	}

}
