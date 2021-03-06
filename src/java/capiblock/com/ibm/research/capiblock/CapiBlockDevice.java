/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/com/ibm/research/capiblock/CapiBlockDevice.java $
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
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.research.capiblock.Chunk.CapiBlockFuture;

/**
 * The capi block device.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class CapiBlockDevice {

	/**
	 * the block size in Bytes
	 */
	public static final int BLOCK_SIZE = 4096;
	public static final String CAPI_BLOCK_VERBOSE_PROPERTY_NAME = "com.ibm.research.capiblock.verbose"; //$NON-NLS-1$
	public static final String CAPI_BLOCK_EMULATION_PROPERTY_NAME = "com.ibm.research.capiblock.emulation"; //$NON-NLS-1$
	public static final String CAPI_BLOCK_CAPACITY_PROPERTY_NAME = "com.ibm.research.capiblock.capacity"; //$NON-NLS-1$
	
	private static boolean useEmulation = false;
	private static boolean verbose = Boolean.parseBoolean(System.getProperty(CAPI_BLOCK_VERBOSE_PROPERTY_NAME, "false"));

	static {
		initialize();
	}

	// Have this private static method because a static initializer cannot have return in it.
	private static void initialize() {
		final String emulationProperty = System.getProperty(CAPI_BLOCK_EMULATION_PROPERTY_NAME);
		if (emulationProperty != null) {
			useEmulation = Boolean.getBoolean(CAPI_BLOCK_EMULATION_PROPERTY_NAME);
			if (useEmulation) {
				if (verbose) {
					System.err.println("WARNING: CAPI block library will use emulation"); //$NON-NLS-1$
				}
				return;
			}
		}

		final String resourceName = "/linux/" + System.getProperty("os.arch") + "/libcapiblock.so"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final InputStream is = CapiBlockDevice.class.getResourceAsStream(resourceName);
		if (is == null) {
			if (emulationProperty != null && !useEmulation) {
				// The user explicitly disabled the emulation,
				// but cannot find the JNI library, so throw an exception.
				throw new UnsupportedOperationException("Unsupported OS/arch. Cannot find " + resourceName); //$NON-NLS-1$
			} else {
				useEmulation = true;
				if (verbose) {
					System.err.println("WARNING: CAPI block library will use emulation"); //$NON-NLS-1$
				}
				return;
			}
		}
		File tempLib = null;
		boolean loaded = false;
		FileOutputStream out = null;
		try {
			tempLib = File.createTempFile("libcapiblock", ".so"); //$NON-NLS-1$ //$NON-NLS-2$
			out = new FileOutputStream(tempLib);

			final byte[] buf = new byte[4096];
			while (true) {
				int read = is.read(buf);
				if (read == -1) {
					break;
				}
				out.write(buf, 0, read);
			}
			out.close();
			out = null;
			System.load(tempLib.getAbsolutePath());
			loaded = true;
		} catch (final IOException ex) {
			if (emulationProperty != null && !useEmulation) {
				// The user explicitly disabled the emulation,
				// but cannot load the JNI library, so throw an exception.
				throw new ExceptionInInitializerError("Cannot load libcapiblock"); //$NON-NLS-1$
			} else {
				useEmulation = true;
				if (verbose) {
					System.err.println("WARNING: CAPI block library will use emulation"); //$NON-NLS-1$
				}
				return;
			}
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (final IOException ex) {
				// ignore exception during close
			}
			if (tempLib != null && tempLib.exists()) {
				if (!loaded) {
					tempLib.delete();
				} else {
					tempLib.deleteOnExit();
				}
			}
		}
	}

	private static CapiBlockDevice instance;
	private static int capacity;
	private static final AtomicInteger chunkIdGenerator = new AtomicInteger(0);
	private static final Map<String, FileAndCounterPair> files = new HashMap<String, FileAndCounterPair>();

	/**
	 * Get the CapiBlockDevice instance.
	 * 
	 * @return the CapiBlockDevice instance.
	 */
	public static synchronized CapiBlockDevice getInstance() {
		if (instance == null) {
			instance = new CapiBlockDevice();
			if (useEmulation) {
				final String capacityProperty = System.getProperty(CAPI_BLOCK_CAPACITY_PROPERTY_NAME);
				if (capacityProperty == null) {
					throw new IllegalStateException("Necessary property com.ibm.research.capiblock.capacity is not set"); //$NON-NLS-1$
				}
				capacity = Integer.parseInt(capacityProperty);
			}
		}
		return instance;
	}

	/**
	 * Tells whether the CAPI Flash emulation is being used.
	 *
	 * @return true, if the CAPI Flash emulation is being used. false, otherwise.
	 */
	public boolean isEmulated() {
		return useEmulation;
	}

	/**
	 * Open a new chunk.
	 * 
	 * @param path
	 *            the file system path to the chunk.
	 * @return the chunk.
	 * @throws IOException
	 *             when the operation failed.
	 */
	public Chunk openChunk(final String path) throws IOException {
		return openChunk(path, 0);
	}

	/**
	 * Open a new chunk.
	 * 
	 * @param path
	 *            the file system path to the chunk.
	 * @param maxRequests
	 *            the maximum number of asynchronous commands that can be
	 *            enqueued for the adapter at the same time. A value of 0 causes
	 *            the block layer to select the default.
	 * @return the chunk.
	 * @throws IOException
	 *             when the operation failed.
	 */
	public Chunk openChunk(final String path, final int maxRequests)
			throws IOException {
		if (useEmulation) {
			if (maxRequests < 0) {
				throw new IOException("maxRequests must not be negative"); //$NON-NLS-1$
			}
			synchronized (this) {
				RandomAccessFile f = null;
				if (files.containsKey(path)) {
					final FileAndCounterPair pair = files.get(path);
					pair.referenceCounter++;
					f = pair.file;
				} else {
					f = new RandomAccessFile(path, "rws"); //$NON-NLS-1$
					f.setLength((long) BLOCK_SIZE * capacity);
					files.put(path, new FileAndCounterPair(f));
				}
				return new EmulatedChunk(this, chunkIdGenerator.getAndIncrement(), f.getChannel(), path);
			}
		} else {
			return new Chunk(this, open(path, maxRequests));
		}
	}

	synchronized void closeEmulation(final String path) throws IOException {
		if (!useEmulation) {
			throw new IllegalStateException("closeEmulation() is called when CAPI Flash emulation is not used"); //$NON-NLS-1$
		}
		final FileAndCounterPair pair = files.get(path);
		if (pair == null) {
			throw new IllegalStateException("Trying to close a non-existing CAPI Flash emulation file: " + path); //$NON-NLS-1$
		}
		pair.referenceCounter--;
		if (pair.referenceCounter == 0) {
			pair.file.close();
			files.remove(path);
		}
	}

	private class FileAndCounterPair {
		final RandomAccessFile file;
		int referenceCounter;

		FileAndCounterPair(final RandomAccessFile file) {
			this.file = file;
			referenceCounter = 1;
		}
	}

	private native int open(String path, int maxRequests) throws IOException;

	native void close(int chunk_id) throws IOException;

	native long get_lun_size(int chunk_id) throws IOException;

	native long read(int chunk_id, long lba, long nBlocks, ByteBuffer buf)
			throws IOException;

	native long write(int chunk_id, long lba, long nBlocks, ByteBuffer buf)
			throws IOException;

	native CapiBlockFuture readAsync(int chunk_id, long lba, long nBlocks,
			ByteBuffer buf) throws IOException;

	native CapiBlockFuture writeAsync(int chunk_id, long lba, long nBlocks,
			ByteBuffer buf) throws IOException;

	native Stats get_stats(int chunk_id);

	native static void releaseMemory(long address);

}
