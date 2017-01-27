/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/AbstractBlockMap.java $
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The abstract block map class provides the foundation for the different sorts
 * of block map implementations. It is possible to have multiple block map
 * facets for a single kvs device as long as they do not access the same keys
 * concurrently (e.g., by having separate key name spaces).
 *
 * @param <B>
 *            the type if block that is consumed by the concrete block map
 *            implementation.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public abstract class AbstractBlockMap<B extends AbstractBlockMap.Block> {

	/**
	 * determines if the native block implementation requires all byteBuffers
	 * being direct so that no copy is required.
	 */
	private static final boolean zeroCopy = true;

	/** The kvs device the block map is associated with */
	protected final CapiKvDevice kvs;

	/**
	 * Instantiates a new abstract block map.
	 *
	 * @param kvs
	 *            the kvs device
	 */
	public AbstractBlockMap(final CapiKvDevice kvs) {
		this.kvs = kvs;
	}

	// synchronous methods

	/**
	 * Puts a new key/block pair into the store.
	 *
	 * @param key
	 *            the key
	 * @param block
	 *            the block
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void put(final String key, final B block) throws IOException {
		kvs.put(key, block.buf);
	}

	/**
	 * Gets a block for a given key from the store.
	 *
	 * @param key
	 *            the key
	 * @param block the block to store the result in
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public boolean get(final String key, final B block) throws IOException {
		checkNotNull("block", block); //$NON-NLS-1$
		return kvs.get(key, block.buf);
	}

	/**
	 * Deletes a key/block pair from the store.
	 *
	 * @param key
	 *            the key
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public boolean delete(final String key) throws IOException {
		return kvs.delete(key);
	}

	/**
	 * Checks whether the store contains a key/block pair for a given key.
	 *
	 * @param key
	 *            the key
	 * @return true, if successful
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public boolean containsKey(final String key) throws IOException {
		return kvs.contains_key(key);
	}

	// asynchronous methods

	/**
	 * Puts a new key/block pair into the store. This method returns immediately
	 * and calls the callback once the asynchronous call has completed either
	 * successfully of an error has occurred.
	 *
	 * @param key
	 *            the key
	 * @param block
	 *            the block
	 * @param cb
	 *            the callback
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void putAsync(final String key, final Block block,
			final Callback.Put cb) throws IOException {
		if (block == null) {
			throw new IllegalArgumentException("block is null"); //$NON-NLS-1$
		}
		kvs.put_async(key, block.buf, cb);
	}

	public Future<Void> putAsync(final String key, final Block block)
			throws IOException {
		if (block == null) {
			throw new IllegalArgumentException("block is null"); //$NON-NLS-1$
		}

		final CallbackFuture<Void> future = new CallbackFuture<Void>();
		kvs.put_async(key, block.buf, future);

		return future;
	}

	/**
	 * Gets the block for a given key from the store. This method returns
	 * immediately and calls the callback once the asynchronous call has
	 * completed either successfully of an error has occurred.
	 *
	 * @param key
	 *            the key
	 * @param cb
	 *            the callback
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void getAsync(final String key, final B block, final Callback.Get<B> cb)
			throws IOException {
		checkNotNull("block", block); //$NON-NLS-1$
		kvs.get_async(key, block.buf, new Callback<String, ByteBuffer>() {

			public void operationCompleted(final String key,
					final ByteBuffer value) {
				cb.operationCompleted(key, block);
			}

			public void operationFailed(final String key, final Exception e) {
				cb.operationFailed(key, e);
			}

		});
	}

	public Future<B> getAsync(final String key, final B block) throws IOException {
		final GetCallbackFuture future = new GetCallbackFuture(block);
		checkNotNull("block", block); //$NON-NLS-1$
		kvs.get_async(key, block.buf, future);
		return future;
	}

	/**
	 * Deletes a key/block pair from the store. This method returns immediately
	 * and calls the callback once the asynchronous call has completed either
	 * successfully of an error has occurred.
	 *
	 * @param key
	 *            the key
	 * @param cb
	 *            the callback
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void deleteAsync(final String key, final Callback.Delete cb)
			throws IOException {
		kvs.delete_async(key, cb);
	}

	public Future<Boolean> deleteAsync(final String key) throws IOException {
		final CallbackFuture<Boolean> future = new CallbackFuture<Boolean>();
		kvs.delete_async(key, future);
		return future;
	}

	/**
	 * Checks whether the store contains a key/block pair for the given key.
	 * This method returns immediately and calls the callback once the
	 * asynchronous call has completed either successfully of an error has
	 * occurred.
	 *
	 * @param key
	 *            the key
	 * @param cb
	 *            the callback
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void containsKeyAsync(final String key, final Callback.ContainsKey cb)
			throws IOException {
		kvs.contains_key_async(key, cb);
	}

	public Future<Boolean> containsKeyAsync(final String key)
			throws IOException {
		final CallbackFuture<Boolean> future = new CallbackFuture<Boolean>();
		kvs.contains_key_async(key, future);
		return future;
	}

	// key iterator

	/**
	 * Gets an iterator that iterates over all existing keys.
	 *
	 * @return the iterator
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public Iterator<String> getIterator() throws IOException {
		return kvs.get_iterator();
	}

	public Iterable<String> keys() {
		return new Iterable<String>() {
			public Iterator<String> iterator() {
				try {
					return kvs.get_iterator();
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	/**
	 * Creates a new block with the correct block size for the underlying kvs
	 * device.
	 *
	 * @return the block
	 */
	public abstract B createBlock(final int size);

	/*
	 * check that an argument is not null.s
	 */
	private void checkNotNull(final String name, final Object o)
			throws IllegalArgumentException {
		if (o == null) {
			throw new IllegalArgumentException(name + " is null"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Blocks are the basic unit for storing values in the store. BlockMap
	 * implementations subclass this class to implement their specific mechanism
	 * of reading and writing to blocks. This often involves some kind of object
	 * serialization since the map moves Java object out of the heap and stores
	 * them on the kvs device.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public static abstract class Block implements Comparable<Block> {

		/** The buffer */
		protected final ByteBuffer buf;

		/**
		 * Instantiates a new block.
		 *
		 * @param buffer
		 *            the buffer to be wrapped
		 */
		public Block(final ByteBuffer buffer) {
			if (buffer.isDirect()) {
				this.buf = buffer;
			} else {
				if (zeroCopy) {
					throw new IllegalArgumentException(
							"byte buffer must be direct"); //$NON-NLS-1$
				} else {
					this.buf = ByteBuffer.allocateDirect(buffer.limit());
					this.buf.put(buffer);
				}
			}
		}

		/**
		 * Gets the size of the block.
		 *
		 * @return the size
		 */
		public int getSize() {
			return buf.limit();
		}
		
		public void clear() {
			buf.clear();
		}

		/**
		 * Compares this block to another block.
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(final Block o) {
			if (o == null) {
				return 1;
			}
			final ByteBuffer me = buf.duplicate();
			me.rewind();
			final ByteBuffer other = o.buf.duplicate();
			other.rewind();
			return me.compareTo(other);
		}

	}

	static abstract class AbstractCallbackFuture<I, O> implements
			Callback<String, I>, java.util.concurrent.Future<O> {

		protected boolean done;
		protected O result;
		private Exception e;

		public void operationFailed(String key, Exception e) {
			synchronized (this) {
				this.e = e;
				this.done = true;
				notifyAll();
			}
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		public boolean isCancelled() {
			return false;
		}

		public boolean isDone() {
			return done;
		}

		public O get() throws InterruptedException, ExecutionException {
			return get(0);
		}

		public O get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get(unit.toMillis(timeout));
		}

		private O get(final long timeout) throws InterruptedException,
				ExecutionException {
			synchronized (this) {
				while (!done) {
					wait(timeout);
				}
			}

			if (e != null) {
				throw new ExecutionException(e);
			}

			return result;
		}

	}

	class GetCallbackFuture extends AbstractCallbackFuture<ByteBuffer, B> {
		
		GetCallbackFuture(final B block) {
			this.result = block;
		}
		
		public void operationCompleted(String key, ByteBuffer value) {
			synchronized (this) {
				this.done = true;
				notifyAll();
			}
		}
	}

	class CallbackFuture<V> extends AbstractCallbackFuture<V, V> {
		public void operationCompleted(String key, V value) {
			synchronized (this) {
				this.done = true;
				this.result = value;
				notifyAll();
			}
		}
	}

}
