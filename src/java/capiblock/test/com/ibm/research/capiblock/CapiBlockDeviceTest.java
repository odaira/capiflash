/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/test/com/ibm/research/capiblock/CapiBlockDeviceTest.java $
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
package com.ibm.research.capiblock;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for the capiblock Java library
 * 
 * @author Jan S. Rellermeyer, IBM Research
 *
 */
public class CapiBlockDeviceTest {
	
	static final String CAPI_DEVICE_PATH_PROP = "CAPI_DEVICE_PATH"; //$NON-NLS-1$ 

	static final String DEVICE = System.getProperty(CAPI_DEVICE_PATH_PROP, "/dev/sdc"); //$NON-NLS-1$ 

	private CapiBlockDevice cblk;

	@Before
	public void setUp() throws Exception {
		cblk = CapiBlockDevice.getInstance();
	}

	@After
	public void tearDown() throws Exception {
		cblk = null;
	}

	private Random random = new Random();

	private String generateString(final int len) {
		return new BigInteger(len, random).toString();
	}

	@Test
	public void testOpen() {
		try (final Chunk chunk = cblk.openChunk("/dev/foo")) {  //$NON-NLS-1$ 
			fail();
		} catch (final IOException ioe) {
			// expected
		}
	
		try (final Chunk chunk = cblk.openChunk("/dev/null")) {  //$NON-NLS-1$ 
			fail();
		} catch (final IOException ioe) {
			// expected
		}

		try (final Chunk chunk = cblk.openChunk(DEVICE)) {  
			// success
		} catch (final IOException ioe) {
			fail(ioe.getMessage());
		}
		
		try (final Chunk chunk = cblk.openChunk(DEVICE, 0)) {  
			// success
		} catch (final IOException ioe) {
			fail(ioe.getMessage());
		}
		
		try (final Chunk chunk = cblk.openChunk(DEVICE, -1)) {  
			fail();
		} catch (final IOException ioe) {
			// expected
		}
	}
	
	public void testClose() throws IOException {
		final Chunk chunk = cblk.openChunk(DEVICE);

		try {
			chunk.close();
		} catch (final IOException ioe) {
			fail(ioe.getMessage());
		}
		
		try {
			chunk.close();
			fail();
		} catch (final IOException ioe) {
			// expected
		}
	}
	
	@Test
	public void testExceptions() throws Exception {
		final ByteBuffer buf = ByteBuffer.allocateDirect(4096);
		try (final Chunk chunk = cblk.openChunk(DEVICE, 0)) {  
			try {
				chunk.readBlock(-1, 1, buf);
				fail();
			} catch (final IOException ioe) {
				// expected
			}
			try {
				chunk.readBlock(0, -1, buf);
				fail();
			} catch (final IOException ioe) {
				// expected
			}
			try {
				chunk.readBlock(Long.MAX_VALUE, 1, buf);
				fail();
			} catch (final IOException ioe) {
				// expected
			}
			try {
				chunk.readBlock(0, Long.MAX_VALUE, buf);
				fail();
			} catch (final IOException ioe) {
				// expected
			}
		} 
	}
	
	@Test
	public void testSynchronous() throws Exception {
		final Chunk chunk = cblk.openChunk(DEVICE);

		final String str = "THIS IS A TEST"; //$NON-NLS-1$
		
		final ByteBuffer buf = ByteBuffer.allocateDirect(4096);

		final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(str);

		chunk.writeBlock(0, 1L, buffer.buf);

		chunk.close();

		final Chunk chunk2 = cblk.openChunk(DEVICE);

		final long r = chunk2.readBlock(0, 1, buf);

		final ObjectStreamBuffer buffer2 = new ObjectStreamBuffer(buf);

		chunk2.close();

		assertTrue(r == 1);
		assertEquals(buffer2.unwrap(), str);
	}

	@Test
	public void testMultipleSynchronous() throws Exception {
		final Chunk chunk = cblk.openChunk(DEVICE);

		final String str = generateString(10 * 4096);
		
		final ByteBuffer buf = ByteBuffer.allocateDirect(10 * 4096);

		final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(10, str);

		chunk.writeBlock(0, 10, buffer.buf);

		chunk.close();

		final Chunk chunk2 = cblk.openChunk(DEVICE);

		final long r = chunk2.readBlock(0, 10, buf);

		final ObjectStreamBuffer buffer2 = new ObjectStreamBuffer(buf);

		chunk2.close();

		assertTrue(r == 10);
		assertEquals(buffer2.unwrap(), str);
	}

	@Test
	public void testAsynchronousRead() throws Exception {
		final Chunk chunk = cblk.openChunk(DEVICE);

		final String str = "THIS IS A TEST"; //$NON-NLS-1$

		final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(str);

		final long w = chunk.writeBlock(0, 1, buffer.buf);

		chunk.close();

		assertTrue(w == 1);
		
		final Chunk chunk2 = cblk.openChunk(DEVICE);

		final ByteBuffer buf = ByteBuffer.allocateDirect(4096);

		final Future<Long> f = chunk2.readBlockAsync(0, 1, buf);

		final ObjectStreamBuffer buffer2 = new ObjectStreamBuffer(buf);

		chunk2.close();

		assertTrue(f.get().longValue() == 1L);
		assertEquals(buffer2.unwrap(), str);
	}
	
	@Test
	public void testAsynchronousWrite() throws Exception {
		final Chunk chunk = cblk.openChunk(DEVICE);

		final String str = "THIS IS A TEST"; //$NON-NLS-1$

		final ByteBuffer buf = ByteBuffer.allocateDirect(4096);
		
		final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(str);

		final Future<Long> f = chunk.writeBlockAsync(0, 1, buffer.buf);

		final long w = f.get().longValue();
		
		chunk.close();
		
		assertTrue(w == 1);
		
		final Chunk chunk2 = cblk.openChunk(DEVICE);

		final long r = chunk2.readBlock(0, 1, buf);

		final ObjectStreamBuffer buffer2 = new ObjectStreamBuffer(buf);

		chunk2.close();

		assertTrue(r == 1);
		assertEquals(buffer2.unwrap(), str);
	}
	
	@Test
	public void testMultipleAsynchronous() throws Exception {
		final Chunk chunk = cblk.openChunk(DEVICE);

		final String str = generateString(10 * 4096);
		
		final ByteBuffer buf = ByteBuffer.allocateDirect(10 * 4096);

		final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(10, str);

		Future<Long> f = chunk.writeBlockAsync(5, 10, buffer.buf);

		final long w = f.get().longValue();
		
		chunk.close();

		assertTrue(w == 10);
		
		final Chunk chunk2 = cblk.openChunk(DEVICE);

		final Future<Long> f2 = chunk2.readBlockAsync(5, 10, buf);

		final long r = f2.get().longValue();
		
		final ObjectStreamBuffer buffer2 = new ObjectStreamBuffer(buf);

		chunk2.close();

		assertTrue(r == 10);
		assertEquals(buffer2.unwrap(), str);
	}
	
	@Test
	public void testTimeout() throws Exception {
		if (!cblk.useEmulation()) {
			try (final Chunk chunk = cblk.openChunk(DEVICE)) {  //$NON-NLS-1$ 
				final String str = generateString(100 * 4096);

				final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(100, str);

				Future<Long> f = chunk.writeBlockAsync(0, 100, buffer.buf);

				f.get(1, TimeUnit.NANOSECONDS);
			
				fail();
			} catch (final TimeoutException te) {
				// expected
			}
		}
		
		try (final Chunk chunk = cblk.openChunk(DEVICE)) {  //$NON-NLS-1$ 
			final String str = generateString(100 * 4096);

			final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(100, str);

			Future<Long> f = chunk.writeBlockAsync(0, 100, buffer.buf);

			f.get(1, TimeUnit.SECONDS);
		} catch (final TimeoutException te) {
			fail("timeout unexpected"); //$NON-NLS-1$
		}
	}
	
	@Test
	public void testStatus() throws Exception {
		try (final Chunk chunk = cblk.openChunk(DEVICE)) {  //$NON-NLS-1$ 
			final Stats stats = chunk.getStats();
			assertTrue(stats.blockSize == 4096);
			
			final String str = "test content"; //$NON-NLS-1$			
			final ObjectStreamBuffer buffer = ObjectStreamBuffer.wrap(2, str);
			
			chunk.writeBlock(0, 2, buffer.buf);
			
			final Stats stats2 = chunk.getStats();
			assertTrue(stats2.numWrites == stats.numWrites + 1);
			assertTrue(stats2.numBlocksWritten == stats.numBlocksWritten + 2);
			
			final ByteBuffer buf = ByteBuffer.allocateDirect(6 * 4096);

			chunk.readBlock(0, 6, buf);
			
			final Stats stats3 = chunk.getStats();
			
			assertTrue(stats3.numReads == stats.numReads + 1);
			assertTrue(stats3.numBlocksRead == stats.numBlocksRead + 6);
		
			final Future<Long> w = chunk.writeBlockAsync(0, 2, buffer.buf);
			
			w.get();
			
			final Stats stats4 = chunk.getStats();
			assertTrue(stats4.numAWrites == stats.numAWrites + 1);
			assertTrue(stats4.numBlocksWritten == stats3.numBlocksWritten + 2);
		}
	}
	
	@Test
	public void testBlock() throws Exception {
		try (final Chunk chunk = cblk.openChunk(DEVICE)) {  //$NON-NLS-1$ 
			final ByteBuffer buf = ByteBuffer.allocateDirect(1);
			try {
				chunk.readBlock(0, 100, buf);
				fail();
			} catch (final IllegalArgumentException iae) {
				// expected
			}

			try {
				chunk.readBlock(0, 100, null);
				fail();
			} catch (final IllegalArgumentException iae) {
				// expected
			}

			try {
				chunk.writeBlock(0, 100, buf);
				fail();
			} catch (final IllegalArgumentException iae) {
				// expected
			}

			try {
				chunk.writeBlock(0, 100, null);
				fail();
			} catch (final IllegalArgumentException iae) {
				// expected
			}
		}
	}

}
