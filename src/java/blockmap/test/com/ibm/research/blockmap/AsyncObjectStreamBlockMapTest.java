/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/AsyncObjectStreamBlockMapTest.java $
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

import static com.ibm.research.blockmap.CapiTestSupport.DEVICE_PATH;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class AsyncObjectStreamBlockMapTest {

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
	public void testPutGet0() throws IOException, ClassNotFoundException,
			InterruptedException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = blockMap.createBlock(1024);

		final ObjectOutputStream out = b.getOutputStream();
		out.writeObject(s);
		out.close();

		final PutCallback p_cb = new PutCallback(key);
		blockMap.putAsync(key, b, p_cb);
		p_cb.waitForResult();

		final Block b2 = blockMap.createBlock(1024);
		final GetCallback g_cb = new GetCallback(key);
		blockMap.getAsync(key, b2, g_cb);
		final Block result = g_cb.waitForResult();

		assertNotNull(result);

		assertEquals(result.getInputStream().readObject(), s);
		assertTrue(b.compareTo(result) == 0);
	}

	@Test
	public void testPutGet1() throws IOException, ClassNotFoundException,
			InterruptedException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = Block.wrap(blockMap, 1024, s);

		final PutCallback p_cb = new PutCallback(key);
		blockMap.putAsync(key, b, p_cb);
		p_cb.waitForResult();

		final Block b2 = blockMap.createBlock(1024);
		
		final GetCallback g_cb = new GetCallback(key);
		blockMap.getAsync(key, b2, g_cb);
		final Block b3 = g_cb.waitForResult();

		assertNotNull(b2);
		assertNotNull(b3);
		assertEquals(b2, b3);
		
		assertEquals(b2.unwrap(), s);
		assertTrue(b2.compareTo(b) == 0);
	}

	@Test
	public void testPutGet3() throws IOException, ClassNotFoundException,
			InterruptedException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$
		final Long l = new Long(10000000000000000L);
		final Integer i = new Integer(42);

		final Block b = Block.wrapAll(blockMap, 2048, s, l, i);

		final PutCallback p_cb = new PutCallback(key);
		blockMap.putAsync(key, b, p_cb);
		p_cb.waitForResult();

		final Block b2 = blockMap.createBlock(2048);
		final GetCallback g_cb = new GetCallback(key);
		blockMap.getAsync(key, b2, g_cb);
		final Block b3 = g_cb.waitForResult();

		assertNotNull(b2);
		assertNotNull(b3);
		assertEquals(b2, b3);

		assertTrue(b2.compareTo(b) == 0);

		final Object[] res = b2.unwrapMany();
		assertTrue(res.length == 3);
		assertEquals(res[0], s);
		assertEquals(res[1], l);
		assertEquals(res[2], i);
	}

	@Test
	public void testEOF() throws IOException, ClassNotFoundException,
			InterruptedException {
		final String key = "key"; //$NON-NLS-1$
		final String s = "Hello World"; //$NON-NLS-1$

		final Block b = blockMap.createBlock(240);

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

		final PutCallback p_cb = new PutCallback(key);
		blockMap.putAsync(key, b, p_cb);
		p_cb.waitForResult();

		final Block b2 = blockMap.createBlock(240);
		final GetCallback g_cb = new GetCallback(key);
		blockMap.getAsync(key, b2, g_cb);
		final Block b3 = g_cb.waitForResult();

		assertNotNull(b2);
		assertNotNull(b3);
		assertEquals(b2, b3);
		
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
	public void testNonExistingKey() throws IOException, InterruptedException {
		final String key = "none"; //$NON-NLS-1$
		final Block b = blockMap.createBlock(1024);
		final GetCallback g_cb = new GetCallback(key);
		blockMap.getAsync(key, b, g_cb);
		assertTrue(g_cb.waitForResult().buf.limit() == 0);
	}

	@Test
	public void testExceptionInCallback() throws IOException,
			InterruptedException {
		final String key = "none"; //$NON-NLS-1$

		final Block b = blockMap.createBlock(1024);
		
		final FaultyCallback cb = new FaultyCallback();

		blockMap.getAsync(key, b, cb);

		cb.waitForThread();
	}

	@Test
	public void testDelete() throws IOException, InterruptedException {
		final String KEY = "MyTestKey"; //$NON-NLS-1$
		final String NEG_KEY = "doesNotExist"; //$NON-NLS-1$
		final Block block = Block.wrap(blockMap, 100, "Hello World"); //$NON-NLS-1$

		blockMap.put(KEY, block);

		final Block b = blockMap.createBlock(100);
		
		// test that the block is there
		assertTrue(blockMap.get(KEY, b));
		assertTrue(b.buf.limit() > 0);

		final DeleteAndContainsKeyCallback cb = new DeleteAndContainsKeyCallback(
				KEY);
		final DeleteAndContainsKeyCallback cb2 = new DeleteAndContainsKeyCallback(
				NEG_KEY);

		// delete the block
		blockMap.deleteAsync(KEY, cb);
		assertTrue(cb.waitForResult());

		// check that the block is gone
		assertFalse(blockMap.get(KEY, b));

		// check non-existing key
		try {
			blockMap.deleteAsync(NEG_KEY, cb2);
			assertFalse(cb2.waitForResult());
		} catch (final Throwable t) {
			fail();
		}
	}

	@Test
	public void testContainsKey() throws IOException, InterruptedException {
		final String KEY = "TestKey"; //$NON-NLS-1$
		final String NEG_KEY = "noSuchKey"; //$NON-NLS-1$

		final Block block = Block.wrap(blockMap, 100, new HashMap<Object, Object>());

		blockMap.put(KEY, block);

		final DeleteAndContainsKeyCallback cb = new DeleteAndContainsKeyCallback(
				KEY);
		final DeleteAndContainsKeyCallback cb2 = new DeleteAndContainsKeyCallback(
				NEG_KEY);
		final DeleteAndContainsKeyCallback cb3 = new DeleteAndContainsKeyCallback(
				KEY);

		blockMap.containsKeyAsync(KEY, cb);
		assertTrue(cb.waitForResult());

		blockMap.containsKeyAsync(NEG_KEY, cb2);
		assertFalse(cb2.waitForResult());

		assertTrue(blockMap.delete(KEY));

		blockMap.containsKeyAsync(KEY, cb3);
		assertFalse(cb3.waitForResult());
	}
	
	@Test
	public void testNoCallback() throws IOException {
		final String KEY = "this is a key"; //$NON-NLS-1$
		
		final Block block = Block.wrap(blockMap, 100, Boolean.TRUE);
		
		// test put
		blockMap.putAsync(KEY, block, null);
		
		// test get
		blockMap.getAsync(KEY, block);
		
		// test containsKey
		blockMap.containsKeyAsync(KEY, null);
		
		// test delete
		blockMap.deleteAsync(KEY, null);
	}

	@Test
	public void testGetFuture() throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
		final String KEY = "123456"; //$NON-NLS-1$
		
		blockMap.put(KEY, Block.wrap(blockMap, 100, new Integer(1)));

		final Block b = blockMap.createBlock(100);
		
		final Future<Block> future = blockMap.getAsync(KEY, b);

		assertTrue(((Integer) future.get().unwrap()).intValue() == 1);
	}
	
	// TODO: test the other futures, test exceptions
	
	protected class GetCallback implements Callback.Get<Block> {

		private final String key;
		private Throwable t;
		private boolean done;
		private Block result;

		protected GetCallback(final String key) {
			this.key = key;
		}

		public void operationCompleted(final String _key, final Block value) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			result = value;
			synchronized (this) {
				done = true;
				notifyAll();
			}
		}

		public void operationFailed(final String _key, final Exception _e) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			synchronized (this) {
				t = _e;
				done = true;
				notifyAll();
			}
		}

		protected Block waitForResult() throws InterruptedException {
			synchronized (this) {
				while (!done) {
					wait();
				}
			}

			assertNull(t);
			return result;
		}

	}

	protected class PutCallback implements Callback.Put {

		private final String key;
		private Throwable t;
		private boolean done;

		protected PutCallback(final String key) {
			this.key = key;
		}

		public void operationCompleted(final String _key, final Void value) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			synchronized (this) {
				done = true;
				notifyAll();
			}
		}

		public void operationFailed(final String _key, final Exception _e) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			synchronized (this) {
				done = true;
				t = _e;
				notifyAll();
			}
		}

		protected void waitForResult() throws InterruptedException {
			synchronized (this) {
				while (!done) {
					wait();
				}
			}

			assertNull(t);
		}

	}

	protected class DeleteAndContainsKeyCallback implements Callback.Delete,
			Callback.ContainsKey {

		private final String key;
		private Throwable t;
		private boolean done;
		private boolean result;

		protected DeleteAndContainsKeyCallback(final String key) {
			this.key = key;
		}

		public void operationCompleted(final String _key, final Boolean value) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			synchronized (this) {
				result = value.booleanValue();
				done = true;
				notifyAll();
			}
		}

		public void operationFailed(final String _key, final Exception _e) {
			try {
				assertEquals(_key, key);
			} catch (final AssertionError ae) {
				t = ae;
			}

			synchronized (this) {
				done = true;
				t = _e;
				notifyAll();
			}
		}

		protected boolean waitForResult() throws InterruptedException {
			synchronized (this) {
				while (!done) {
					wait();
				}
			}

			assertNull(t);

			return result;
		}

	}

	protected class FaultyCallback implements
			Callback.Get<ObjectStreamBlockMap.Block> {
		Thread thread;

		public void operationCompleted(final String key, final Block value) {
			synchronized (this) {
				thread = Thread.currentThread();
				notifyAll();
			}
			throw new RuntimeException();
		}

		public void operationFailed(final String key, final Exception e) {
			fail();
		}

		protected void waitForThread() throws InterruptedException {
			synchronized (this) {
				while (thread == null) {
					wait();
				}
			}

			// wait for the exception to be thrown (race condition)
			Thread.sleep(1000);

			if (thread == null || !thread.isAlive()) {
				fail();
			}
		}
	}

}
