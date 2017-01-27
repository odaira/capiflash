/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/ObjectStreamBlockTest.java $
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class ObjectStreamBlockTest {

	private Block b;

	@Before
	public void setUp() throws Exception {
		b = new ObjectStreamBlockMap.Block(1024);
	}

	@After
	public void tearDown() throws Exception {
		b = null;
	}

	@Test
	public void testInput() throws IOException {
		// create an input stream header
		// so that we can open the input stream first
		b.buf.putShort((short) 0xaced);
		b.buf.putShort((short) 5);
		b.buf.flip();

		b.getInputStream();

		try {
			b.getOutputStream();
		} catch (final IllegalStateException e) {
			// expected
			return;
		}

		fail();
	}

	@Test
	public void testOutput() throws IOException {
		b.getOutputStream();

		try {
			b.getInputStream();
		} catch (final IllegalStateException e) {
			// expected
			return;
		}

		fail();
	}
	
	@Test
	public void testInputOutput() throws IOException {
		final ObjectOutputStream out = b.getOutputStream();
		out.writeInt(1);
		
		final ObjectOutputStream out2 = b.getOutputStream();
		out.writeInt(2);
		out2.writeInt(3);
		
		out.close();
		out2.close();
		
		final ObjectInputStream in = b.getInputStream();
		assertTrue(in.readInt() == 1);
		assertTrue(in.readInt() == 2);
		assertTrue(in.readInt() == 3);
		in.close();
		
		final ObjectOutputStream out3 = b.getOutputStream();
		out3.writeInt(10);
		out3.writeInt(11);
		out3.writeInt(12);
		out3.close();
		
		final ObjectInputStream in2 = b.getInputStream();
		assertTrue(in2.readInt() == 10);
		
		final ObjectInputStream in3 = b.getInputStream();
		assertTrue(in3.readInt() == 11);
		assertTrue(in2.readInt() == 12);
		
		in3.close();
		in2.close();
		
		final ObjectOutputStream out4 = b.getOutputStream();
		out4.writeLong(10000000L);
		out4.close();
		
		final ObjectInputStream in4 = b.getInputStream();
		assertTrue(in4.readLong() == 10000000L);
		in4.close();
	}
	

	@Test
	public void testReadWrite() throws IOException, ClassNotFoundException {
		final String s = "Hello World"; //$NON-NLS-1$
		final Long l = new Long(123123131231231L);
		final HashMap<Object, Object> m = new HashMap<Object, Object>();
		m.put(s, l);

		final ObjectOutputStream out = b.getOutputStream();
		out.writeObject("test"); //$NON-NLS-1$
		out.writeObject(s);
		out.writeObject(l);
		out.writeObject(m);
		out.close();

		final ObjectInputStream in = b.getInputStream();
		assertEquals(in.readObject(), "test"); //$NON-NLS-1$
		assertEquals(in.readObject(), s);
		assertEquals(in.readObject(), l);
		assertEquals(in.readObject(), m);
		in.close();
	}
	


}
