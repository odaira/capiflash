/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/com/ibm/research/capiblock/Chunk.java $
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
import java.nio.channels.FileChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Emulated chunk of CAPI Flash, using RandomAccessFile
 * 
 * @author Bedri Sendir, Rei Odaira
 */
public class EmulatedChunk extends Chunk {
	private final FileChannel inChannel;
	private final String path;

	private long numReads = 0;
	private long numWrites = 0;
	private long numAWrites = 0;
	private long numAReads = 0;
	private long numBlocksRead = 0;
	private long numBlocksWritten = 0;

	EmulatedChunk(final CapiBlockDevice cblk, final int chunk_id, final FileChannel inChannel, final String path) {
		super(cblk, chunk_id);
		this.inChannel = inChannel;
		this.path = path;
	}

	/**
	 * Write one or more blocks from a ByteBuffer into the chunk.
	 * 
	 * @param lba
	 *            the local block address to start at.
	 * @param nBlocks
	 *            the number of blocks to write.
	 * @param buf
	 *            the source buffer to read from. Must be a direct buffer.
	 * @return the actual number of blocks written.
	 * @throws IOException
	 *             when the operation failed.
	 */
	@Override
	public long writeBlock(final long lba, final long nBlocks, final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		try {
			buf.rewind();
			buf.limit((int) nBlocks * CapiBlockDevice.BLOCK_SIZE);
			synchronized (inChannel) {
				inChannel.write(buf, (long) lba * CapiBlockDevice.BLOCK_SIZE);
			}
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		synchronized (this) {
			numWrites++;
			numBlocksWritten += nBlocks;
		}
		return nBlocks;
	}

	/**
	 * Read one or more blocks from the chunk into a ByteBuffer.
	 * 
	 * @param lba
	 *            the local block address to start at.
	 * @param nBlocks
	 *            the number of blocks to read.
	 * @param buf
	 *            the destination buffer to read into. Must be a direct buffer.
	 * @return the actual number of blocks read.
	 * @throws IOException
	 *             when the operation failed.
	 */
	@Override
	public long readBlock(final long lba, final long nBlocks, final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		try {
			buf.clear();
			int oldLimit = buf.limit();
			buf.limit((int) (nBlocks * CapiBlockDevice.BLOCK_SIZE));
			synchronized (inChannel) {
				inChannel.read(buf, (long) lba * CapiBlockDevice.BLOCK_SIZE);
			}
			buf.limit(oldLimit);
			buf.rewind();
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		synchronized (this) {
			numReads++;
			numBlocksRead += nBlocks;
		}
		return nBlocks;
	}

	/**
	 * Asynchronously read one or more blocks from the chunk into a ByteBuffer.
	 * This method returns as soon as the command could be enqueued. The content
	 * of the destination buffer is not valid until the future has identified
	 * the operation as completed.
	 * 
	 * @param lba
	 *            the local block address to start at.
	 * @param nBlocks
	 *            the number of blocks to read.
	 * @param buf
	 *            the destination buffer to read into. Must be a direct buffer.
	 * @return a Future for the actual number of blocks read.
	 * @throws IOException
	 *             when the operation failed.
	 */
	@Override
	public Future<Long> readBlockAsync(final long lba, final long nBlocks, final ByteBuffer buf) throws IOException {
		long retVal = readBlock(lba, nBlocks, buf);
		synchronized (this) {
			numAReads++;
			numReads--;
		}
		CompletableFuture<Long> future = new CompletableFuture<Long>();
		future.complete(retVal);
		return (Future<Long>) future;
	}

	/**
	 * Asynchronously write one or more blocks from a ByteBuffer into the chunk.
	 * This method returns as soon as the command could be enqueued. The content
	 * of the blocks in the chunk is not valid until the future has identified
	 * the operation as completed.
	 * 
	 * @param lba
	 *            the local block address to start at.
	 * @param nBlocks
	 *            the number of blocks to write.
	 * @param buf
	 *            the source buffer to read from. Must be a direct buffer.
	 * @return a Future for the actual number of blocks written.
	 * @throws IOException
	 *             when the operation failed.
	 */
	@Override
	public Future<Long> writeBlockAsync(final long lba, final long nBlocks, final ByteBuffer buf) throws IOException {
		long retVal = writeBlock(lba, nBlocks, buf);
		synchronized (this) {
			numAWrites++;
			numWrites--;
		}
		CompletableFuture<Long> future = new CompletableFuture<Long>();
		future.complete(retVal);
		return (Future<Long>) future;
	}

	/**
	 * get the size of the chunk in blocks.
	 *
	 * @return the size of the chunk as a number of blocks.
	 * @throws IOException
	 *             when the operation failed.
	 */
	@Override
	public long getSize() throws IOException {
		return inChannel.size() / CapiBlockDevice.BLOCK_SIZE;
	}

	/**
	 * closes the chunk. This call might fail, e.g., if there are still
	 * asynchronous operations in flight and those operations do not complete
	 * within a wait time.
	 * 
	 * @throws IOException
	 *             when the close operation failed.
	 */
	@Override
	public void close() throws IOException {
		CapiBlockDevice.getInstance().closeEmulation(path);
	}

	/**
	 * Get the statistics for this chunk.
	 * 
	 * @return a Stats POJO with a snapshot of the statistics.
	 */
	@Override
	public Stats getStats() {
		return new Stats(CapiBlockDevice.BLOCK_SIZE, 1,
				 Long.MAX_VALUE, numReads,
				 numWrites, numAReads, numAWrites,
				 0, 0,
				 0, 0,
				 1, 1,
				 1, 1,
				 numBlocksRead, numBlocksWritten,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0,
				 0, 0);
	}
}
