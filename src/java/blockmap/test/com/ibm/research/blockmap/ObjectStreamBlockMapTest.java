/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/ObjectStreamBlockMapTest.java $
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

import static com.ibm.research.blockmap.CapiTestSupport.DEVICE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class ObjectStreamBlockMapTest {

	private CapiKvDevice kvs;
	private ObjectStreamBlockMap blockMap;

	@Before
	public void setUp() throws Exception {
		kvs = new CapiKvDevice(DEVICE_PATH);
		blockMap = new ObjectStreamBlockMap(kvs);
	}

	@After
	public void tearDown() throws Exception {
		kvs.close();
		kvs = null;
		blockMap = null;
	}

	@Test
	public void testPutGet0() throws IOException, ClassNotFoundException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = blockMap.createBlock(4096);

		final ObjectOutputStream out = b.getOutputStream();
		out.writeObject(s);
		out.close();

		final Block b2 = blockMap.createBlock(4096);
		blockMap.put(key, b);

		assertTrue(blockMap.get(key, b2));

		assertEquals(b2.getInputStream().readObject(), s);
		assertTrue(b2.compareTo(b) == 0);
	}

	@Test
	public void testPutGet1() throws IOException, ClassNotFoundException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = Block.wrap(blockMap, 222, s);

		blockMap.put(key, b);

		final Block b2 = blockMap.createBlock(222);
				
		assertTrue(blockMap.get(key, b2));

		assertEquals(b2.unwrap(), s);
		assertTrue(b2.compareTo(b) == 0);
	}

	@Test
	public void testPutGet3() throws IOException, ClassNotFoundException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$
		final Long l = new Long(10000000000000000L);
		final Integer i = new Integer(42);

		final Block b = Block.wrapAll(blockMap, 1024, s, l, i);

		blockMap.put(key, b);

		final Block b2 = blockMap.createBlock(1024);
				
		assertTrue(blockMap.get(key, b2));

		assertTrue(b2.compareTo(b) == 0);

		final Object[] res = b2.unwrapMany();
		assertTrue(res.length == 3);
		assertEquals(res[0], s);
		assertEquals(res[1], l);
		assertEquals(res[2], i);
	}

	@Test
	public void testEOF() throws IOException, ClassNotFoundException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = blockMap.createBlock(512);

		final ObjectOutputStream out = b.getOutputStream();
		out.writeObject(s);
		out.close();

		final ObjectInputStream in = b.getInputStream();
		assertEquals(in.readObject(), s);

		try {
			in.readObject();
			fail("expected IOException"); //$NON-NLS-1$
		} catch (final IOException ioe) {
			// expected
		}
		in.close();

		blockMap.put(key, b);

		final Block b2 = blockMap.createBlock(256);
		assertTrue(blockMap.get(key, b2));

		assertEquals(b2.getInputStream().readObject(), s);
		assertTrue(b2.compareTo(b) == 0);

		try {
			b2.getInputStream().readObject();
			fail("expected IOException"); //$NON-NLS-1$
		} catch (final IOException ioe) {
			// expected
		}
	}

	@Test
	public void testNonExistingKey() throws IOException {
		assertFalse(blockMap.get("none", blockMap.createBlock(1))); //$NON-NLS-1$
	}

	@Test
	public void testInvalidKey() throws IOException {
		try {
			blockMap.get(null, null);
		} catch (final IllegalArgumentException e) {
			return;
		}
		fail();
	}

	@Test
	public void testDelete() throws IOException {
		final String KEY = "MyTestKey"; //$NON-NLS-1$
		final Block block = Block.wrap(blockMap, 256, "Hello World"); //$NON-NLS-1$

		blockMap.put(KEY, block);

		final Block b = blockMap.createBlock(256);
		
		// test that the block is there
		assertTrue(blockMap.get(KEY, b));

		// delete the block
		assertTrue(blockMap.delete(KEY));

		assertFalse(blockMap.get(KEY, b));

		// check non-existing key
		try {
			assertFalse(blockMap.delete("doesNotExist")); //$NON-NLS-1$
		} catch (final Throwable t) {
			fail();
		}
	}

	@Test
	public void testContainsKey() throws IOException {
		final String KEY = "TestKey"; //$NON-NLS-1$

		final Block block = Block.wrap(blockMap, 2048, new HashMap<Object, Object>());

		blockMap.put(KEY, block);

		assertTrue(blockMap.containsKey(KEY));

		assertFalse(blockMap.containsKey("noSuchKey")); //$NON-NLS-1$

		assertTrue(blockMap.delete(KEY));

		assertFalse(blockMap.containsKey(KEY));
	}

	@Test
	public void testIterator() throws IOException {
		// TODO: this test exploits the current iteration order and might break
		// in future versions
		final String[] keys = { "TestKey1", "TestKey2", "TestKey3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final Block[] blocks = { Block.wrap(blockMap, 100, new Integer(1)),
				Block.wrap(blockMap, 100, new Integer(2)),
				Block.wrap(blockMap, 100, new Integer(3)) };

		for (int i = 0; i < keys.length; i++) {
			blockMap.put(keys[i], blocks[i]);
		}

		final Block b = blockMap.createBlock(100);
		
		int i = 0;
		for (final String key : blockMap.keys()) {
			assertEquals(key, keys[i]);
			assertTrue(blockMap.get(key, b));
			assertTrue(b.compareTo(blocks[i]) == 0);
			i++;
		}
	}

	@Test
	public void testClosed() throws IOException {
		kvs.close();

		try {
			blockMap.put("test", Block.wrap(blockMap, 20, "foo")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (final IOException ioe) {
			return;
		}

		fail();
	}

}
