/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/PlainTest.java $
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
package com.ibm.research.blockmap;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Ignore;

import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

import static com.ibm.research.blockmap.CapiTestSupport.DEVICE_PATH;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
@Ignore
public class PlainTest {

	public static void main(final String... args) throws Exception {
		final CapiKvDevice kvs = new CapiKvDevice(DEVICE_PATH);
		final ObjectStreamBlockMap blockMap = new ObjectStreamBlockMap(kvs);

		final String KEY = "key"; //$NON-NLS-1$
		final String KEY2 = "key2"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		Block b = blockMap.createBlock(4096);

		final ObjectOutputStream out = b.getOutputStream();
		out.writeObject("test"); //$NON-NLS-1$
		out.writeObject(s);
		out.close();

		blockMap.put(KEY, b);

		b.clear();
		blockMap.get(KEY, b);

		final ObjectInputStream in = b.getInputStream();
		System.out.println(in.readObject());
		System.out.println(in.readObject());
		in.close();

		blockMap.put(KEY2, b);

		b.clear();
		
		blockMap.get(KEY2, b);

		final ObjectInputStream in2 = b.getInputStream();
		System.out.println(in2.readObject());
		System.out.println(in2.readObject());
		in2.close();

		kvs.close();
	}

}
