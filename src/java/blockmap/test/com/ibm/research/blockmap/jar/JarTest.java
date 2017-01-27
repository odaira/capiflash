/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/test/java/blockmap/com/ibm/research/blockmap/jar/JarTest.java $
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
package com.ibm.research.blockmap.jar;

import java.io.IOException;

import com.ibm.research.blockmap.CapiKvDevice;
import com.ibm.research.blockmap.ObjectStreamBlockMap;
import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class JarTest {

	private static final String KEY = "key"; //$NON-NLS-1$

	public static void main(final String[] args) throws IOException {

		final CapiKvDevice dev = new CapiKvDevice(null);
		final ObjectStreamBlockMap blockMap = new ObjectStreamBlockMap(dev);

		final Block block = Block.wrap(blockMap, 4096,
				new Long(System.currentTimeMillis()));
		blockMap.put(KEY, block);
		dev.close();
		
		System.err.println("### JarTest SUCCESS ###"); //$NON-NLS-1$
	}

}
