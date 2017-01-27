/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/kryo/KryoBlockTest.java $
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
package com.ibm.research.blockmap.kryo;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class KryoBlockTest {

	private KryoBlockMap.Block block;

	@Before
	public void setUp() throws Exception {
		block = new KryoBlockMap.Block(1024);
	}

	@After
	public void tearDown() throws Exception {
		block = null;
	}

	@Test
	public void test() {
		final Kryo kryo = new Kryo();

		final String s = "Hello World"; //$NON-NLS-1$
		final Long l = new Long(123123131231231L);
		final HashMap<Object, Object> m = new HashMap<Object, Object>();
		m.put(s, l);

		final Output out = block.getOutput();
		kryo.writeObject(out, "test"); //$NON-NLS-1$
		kryo.writeObject(out, s);
		kryo.writeObject(out, l);
		kryo.writeObject(out, m);
		out.close();

		final Input in = block.getInput();
		assertEquals(kryo.readObject(in, String.class), "test"); //$NON-NLS-1$
		assertEquals(kryo.readObject(in, String.class), s);
		assertEquals(kryo.readObject(in, Long.class), l);
		assertEquals(kryo.readObject(in, HashMap.class), m);
		in.close();
	}

}
