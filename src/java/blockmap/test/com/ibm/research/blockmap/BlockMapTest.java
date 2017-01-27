/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/BlockMapTest.java $
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.research.blockmap.BlockMap.Block;

import static com.ibm.research.blockmap.CapiTestSupport.DEVICE_PATH;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class BlockMapTest {

	private CapiKvDevice kvs;
	private BlockMap blockMap;

	@Before
	public void setUp() throws Exception {
		kvs = new CapiKvDevice(DEVICE_PATH);
		blockMap = new BlockMap(kvs);
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

		final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
		final double d = 123712363324432234271324234342432323897982343d;
		final String s = "Hello World"; //$NON-NLS-1$
		buf.putDouble(d);
		buf.put(s.getBytes());

		final Block b = blockMap.createBlock(buf);

		blockMap.put(key, b);
		
		final Block b2 = blockMap.createBlock(1024);

		assertTrue(blockMap.get(key, b2));

		assertTrue(b2.compareTo(b) == 0);
		final ByteBuffer buf2 = b2.getBuffer();
		assertTrue(buf2.getDouble() == d);
		final byte[] dst = new byte[s.length()];
		buf2.get(dst);
		assertEquals(s, new String(dst));
	}

	@Test
	public void testNonExistingKey() throws IOException {
		final Block b = blockMap.createBlock(10);
		b.buf.putChar('b');
		assertFalse(blockMap.get("none", b)); //$NON-NLS-1$
		assertTrue(b.buf.limit() == 0);
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

		final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
		final Block block = blockMap.createBlock(buf);

		blockMap.put(KEY, block);

		final Block b2 = blockMap.createBlock(1024);
		
		// test that the block is there
		assertTrue(blockMap.get(KEY, b2));

		// delete the block
		assertTrue(blockMap.delete(KEY));

		assertFalse(blockMap.get(KEY, b2));
		assertTrue(b2.buf.limit() == 0);

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

		final ByteBuffer buf = ByteBuffer.allocateDirect(222);
		final Block block = blockMap.createBlock(buf);

		blockMap.put(KEY, block);

		assertTrue(blockMap.containsKey(KEY));

		assertFalse(blockMap.containsKey("noSuchKey")); //$NON-NLS-1$

		assertTrue(blockMap.delete(KEY));

		assertFalse(blockMap.containsKey(KEY));
	}

	private Block createBlock(final int i) {
		final ByteBuffer buf = ByteBuffer.allocateDirect(100);
		buf.putInt(i);
		return blockMap.createBlock(buf);
	}

	@Test
	public void testIterator() throws IOException {
		// TODO: this test exploits the current iteration order and might break
		// in future versions

		final String[] keys = { "TestKey1", "TestKey2", "TestKey3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final Block[] blocks = { createBlock(0), createBlock(1), createBlock(2) };

		for (int i = 0; i < keys.length; i++) {
			blockMap.put(keys[i], blocks[i]);
		}

		int i = 0;
		final Block b2 = blockMap.createBlock(100);
		for (final String key : blockMap.keys()) {
			assertEquals(key, keys[i]);
			b2.clear();
			assertTrue(blockMap.get(key, b2));
			assertTrue(b2.getBuffer().getInt() == i);
			i++;
		}
	}

	@Test
	public void testClosed() throws IOException {
		kvs.close();

		try {
			blockMap.put("test", createBlock(10)); //$NON-NLS-1$
		} catch (final IOException ioe) {
			return;
		}

		fail();
	}

}
