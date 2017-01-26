/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: Chunk.java $
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Chunk represents a collection of contiguous blocks persistently stored on a
 * CAPI Flash device.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class Chunk implements AutoCloseable {

	private final CapiBlockDevice cblk;

	private final int chunk_id;

	Chunk(final CapiBlockDevice cblk, final int chunk_id) {
		this.cblk = cblk;
		this.chunk_id = chunk_id;
	}

	/**
	 * closes the chunk. This call might fail, e.g., if there are still
	 * asynchronous operations in flight and those operations do not complete
	 * within a wait time.
	 * 
	 * @throws IOException
	 *             when the close operation failed.
	 */
	public void close() throws IOException {
		cblk.close(chunk_id);
	}

	/**
	 * get the size of the chunk in blocks.
	 * 
	 * @return the size of the chunk as a number of blocks.
	 * @throws IOException
	 *             when the operation failed.
	 */
	public long getSize() throws IOException {
		return cblk.get_lun_size(chunk_id);
	}

	// check a buffer
	private static final void checkBuffer(final long nBlocks,
			final ByteBuffer buf) {
		if (buf == null) {
			throw new IllegalArgumentException("buffer must not be null."); //$NON-NLS-1$
		}
		if (!buf.isDirect()) {
			throw new IllegalArgumentException("buffer must be direct."); //$NON-NLS-1$
		}
		if (buf.capacity() < nBlocks * CapiBlockDevice.BLOCK_SIZE) {
			throw new IllegalArgumentException(
					"buffer has insufficient capacity for " + nBlocks + " blocks."); //$NON-NLS-1$ //$NON-NLS-2$
		}
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
	public long readBlock(final long lba, final long nBlocks,
			final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		return cblk.read(chunk_id, lba, nBlocks, buf);
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
	public long writeBlock(final long lba, final long nBlocks,
			final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		return cblk.write(chunk_id, lba, nBlocks, buf);
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
	public Future<Long> readBlockAsync(final long lba, final long nBlocks,
			final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		return cblk.readAsync(chunk_id, lba, nBlocks, buf);
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
	public Future<Long> writeBlockAsync(final long lba, final long nBlocks,
			final ByteBuffer buf) throws IOException {
		checkBuffer(nBlocks, buf);
		return cblk.writeAsync(chunk_id, lba, nBlocks, buf);
	}

	/**
	 * Get the statistics for this chunk.
	 * 
	 * @return a Stats POJO with a snapshot of the statistics.
	 */
	public Stats getStats() {
		return cblk.get_stats(chunk_id);
	}

	protected static final sun.misc.Unsafe unsafe;

	static {
		java.lang.reflect.Constructor<sun.misc.Unsafe> c;
		try {
			c = sun.misc.Unsafe.class.getDeclaredConstructor();
			c.setAccessible(true);
			unsafe = c.newInstance();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A Future for the actual number of blocks read/written by an asynchronous
	 * operation.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 */
	static final class CapiBlockFuture implements Future<Long> {

		// pointer to the beginning of the status struct
		private final long status_addr;
		// materialized for quick(er) access
		private final long block_addr;
		private final long errno_addr;

		CapiBlockFuture(final long status_addr, final long block_addr,
				final long errno_addr) {
			this.status_addr = status_addr;
			this.block_addr = block_addr;
			this.errno_addr = errno_addr;
		}

		public void finalize() {
			// unsafe.freeMemory(status_addr) does not work
			CapiBlockDevice.releaseMemory(status_addr);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			// cblk operations are not cancellable.
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isCancelled() {
			// cblk operations are not cancellable.
			return false;
		}

		@Override
		public boolean isDone() {
			return unsafe.getLong(status_addr) != 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Long get() throws InterruptedException, ExecutionException {
			try {
				return get(0);
			} catch (final TimeoutException e) {
				// does not happen
				return null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Long get(final long timeout, final TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return get(unit.toNanos(timeout));
		}

		private Long get(final long nanos) throws TimeoutException,
				ExecutionException {
			long status = 0;
			if (nanos == 0) {
				while ((status = unsafe.getLong(status_addr)) == 0) {
					// loop
				}
			} else {
				long time = System.nanoTime();
				do {
					if (System.nanoTime() - time > nanos) {
						break;
					}
				} while ((status = unsafe.getLong(status_addr)) == 0);
			}

			if (status == 0) {
				throw new TimeoutException("Future timed out"); //$NON-NLS-1$
			}
			if (status == 1) {
				return new Long(unsafe.getLong(block_addr));
			}
			if (status == 2) {
				throw new IllegalArgumentException(
						"Asynchronous command was invalid, error code=" //$NON-NLS-1$
								+ unsafe.getInt(errno_addr));
			}

			throw new ExecutionException(
					new IOException(
							"Asynchronous command returned with error, status=" + status + ", code=" //$NON-NLS-1$ //$NON-NLS-2$
									+ unsafe.getInt(errno_addr)));
		}
	}
}
