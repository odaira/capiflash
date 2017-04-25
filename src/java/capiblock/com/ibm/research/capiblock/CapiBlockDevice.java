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

	static {
		String resourceName = "/linux/" + System.getProperty("os.arch") + "/libcapiblock.so";
		InputStream is = CapiBlockDevice.class.getResourceAsStream(resourceName);
		if (is == null) {
			throw new UnsupportedOperationException("Unsupported OS/arch. Cannot find " + resourceName);
		}
		File tempLib = null;
		boolean loaded = false;
		FileOutputStream out = null;
		try {
			tempLib = File.createTempFile("libcapiblock", ".so");
			out = new FileOutputStream(tempLib);

			byte[] buf = new byte[4096];
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
		} catch (IOException ex) {
			throw new ExceptionInInitializerError("Cannot load libcapiblock");
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
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

	/**
	 * Get the CapiBlockDevice instance.
	 * 
	 * @return the CapiBlockDevice instance.
	 */
	public static CapiBlockDevice getInstance() {
		if (instance == null) {
			instance = new CapiBlockDevice();
		}
		return instance;
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
	public Chunk openChunk(final String path, int maxRequests)
			throws IOException {
		return new Chunk(this, open(path, maxRequests));
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
