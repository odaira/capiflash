/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/test/blockmap/com/ibm/research/blockmap/MTObjectStreamBlockMapTest.java $
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

import static com.ibm.research.blockmap.CapiTestSupport.DEVICE_PATH;

/**
 * @author Jan S. Rellermeyer, IBM Research
 */
public class MTObjectStreamBlockMapTest {

	private CapiKvDevice kvs;
	protected ObjectStreamBlockMap blockMap;

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

	class Worker implements Runnable {

		private final Random rnd = new Random();
		private final int start;
		private final int end;
		private final int writeCycles;
		private final int readCycles;
		private final int range;

		Worker(final int start, final int end, final int range,
				final int writeCycles, final int readCycles) {
			this.start = start;
			this.end = end;
			this.range = range;
			this.writeCycles = writeCycles;
			this.readCycles = readCycles;
		}

		public void run() {
			try {

				for (int i = 0; i < writeCycles; i++) {
					final int n = rnd.nextInt(end - start);
					final Block b_in = Block.wrap(blockMap, 4096, new Integer(n));
					blockMap.put(String.valueOf(n), b_in);
				}
				
				final Block b_out = blockMap.createBlock(4096);

				for (int i = 0; i < readCycles; i++) {
					final int n = rnd.nextInt(range);
					final boolean exists = blockMap.get(String.valueOf(n), b_out);
					if (exists) {
						assertTrue(b_out.<Integer> unwrap().intValue() == n);
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}

	}

	@Test
	public void testWriteRead() throws Exception {
		final int numThreads = 10;
		final int perThreadRange = 2000;
		final int readCycles = 10000;
		final int writeCycles = 1000;

		final Thread[] threads = new Thread[numThreads];

		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(new Worker(i * perThreadRange, (i + 1)
					* perThreadRange, numThreads * perThreadRange, writeCycles,
					readCycles));
			threads[i].start();
		}

		for (int i = 0; i < numThreads; i++) {
			threads[i].join();
		}
	}

	class RandWorker implements Runnable {

		private final Random rnd = new Random();
		private final int start;
		private final int end;
		private final int cycles;
		private final float writeRatio;
		private final int range;

		RandWorker(final int start, final int end, final int range,
				final int cycles, final float writeRatio) {
			this.start = start;
			this.end = end;
			this.range = range;
			this.cycles = cycles;
			this.writeRatio = writeRatio;
		}

		public void run() {
			try {
				final Block b_out = blockMap.createBlock(100);
				
				for (int i = 0; i < cycles; i++) {
					final boolean write = rnd.nextFloat() <= writeRatio;

					if (write) {
						final int n = rnd.nextInt(end - start);
						final Block b_in = Block.wrap(blockMap, 100, new Integer(n));
						blockMap.put(String.valueOf(n), b_in);
					} else {
						final int n = rnd.nextInt(range);
						final boolean exists = blockMap.get(String.valueOf(n), b_out);
						if (exists) {
							assertTrue(b_out.<Integer> unwrap().intValue() == n);
						}
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}

	@Test
	public void testRandomReadWrite() throws Exception {
		final int numThreads = 10;
		final int perThreadRange = 2000;
		final int cycles = 20000;
		final float writeRatio = 0.25f;

		final Thread[] threads = new Thread[numThreads];

		for (int i = 0; i < numThreads; i++) {
			threads[i] = new Thread(new RandWorker(i * perThreadRange, (i + 1)
					* perThreadRange, numThreads * perThreadRange, cycles,
					writeRatio));
			threads[i].start();
		}

		for (int i = 0; i < numThreads; i++) {
			threads[i].join();
		}
	}
}
