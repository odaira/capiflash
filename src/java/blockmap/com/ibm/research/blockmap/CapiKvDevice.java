/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/CapiKvDevice.java $
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The class provides handle to a Kvs device associated with a capiflash device
 * path.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public final class CapiKvDevice implements Closeable, AutoCloseable {

	static {
		System.loadLibrary("blockmap"); //$NON-NLS-1$
	}

	/** The ark. */
	private final long ark;

	/** The closed. */
	private boolean closed;

	public enum Flag {

		ARK_KV_VIRTUAL_LUN(1 << 0), ARK_KV_PERSIST_STORE(1 << 1), ARK_KV_PERSIST_LOAD(
				1 << 2);

		private final long value;

		Flag(final long value) {
			this.value = value;
		}

		static long getValue(Set<Flag> flags) {
			if (flags == null)
				return 0;

			long value = 0;
			for (final Flag flag : flags) {
				value |= flag.value;
			}
			return value;
		}
	}

	/**
	 * Instantiates a new kvs device.
	 *
	 * @param path
	 *            the file system path to the device or <code>null</code> to run
	 *            the surelock backend against memory.
	 */
	public CapiKvDevice(final String path) {
		this(path, null);
	}

	/**
	 * Instantiates a new capi kv device.
	 *
	 * @param path
	 *            the file system path to the device or <code>null</code> to run
	 *            the surelock backend against memory.
	 * @param flags
	 *            a EnumSet of Flag values to pass to the CapiKvDevice, e.g., to
	 *            enable persistence.
	 */
	public CapiKvDevice(final String path,
			final EnumSet<Flag> flags) {
		ark = create(path, Flag.getValue(flags));
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		if (!closed) {
			close(ark);
			closed = true;
		}
	}

	/*
	 * check that the device has not already been closed.
	 */
	private void checkNotClosed() throws IOException {
		if (closed) {
			throw new IOException("Device has been closed"); //$NON-NLS-1$
		}
	}

	/*
	 * check that an argument is not null.s
	 */
	private void checkNotNull(final String name, final Object o)
			throws IllegalArgumentException {
		if (o == null) {
			throw new IllegalArgumentException(name + " is null"); //$NON-NLS-1$
		}
	}

	/*
	 * Puts a block.
	 * 
	 * @param key the key
	 * 
	 * @param buf the buffer representing the block
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void put(final String key, final ByteBuffer buf) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		checkNotNull("buf", buf); //$NON-NLS-1$
		put(ark, key, buf, buf.limit());
	}

	/*
	 * Gets a block.
	 * 
	 * @param key the key
	 * 
	 * @param buf the buffer to store the result in
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	boolean get(final String key, final ByteBuffer buf) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		return get(ark, key, buf);
	}

	/*
	 * Deletes a key/block pair.
	 * 
	 * @param key the key
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	boolean delete(final String key) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		return delete(ark, key);
	}

	/*
	 * Check if the key exists.
	 * 
	 * @param key the key
	 * 
	 * @return true, if key exists.
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	boolean contains_key(final String key) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		return contains_key(ark, key);
	}

	/*
	 * Puts asynchronously.
	 * 
	 * @param key the key
	 * 
	 * @param buf the block
	 * 
	 * @param cb the callback
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void put_async(final String key, final ByteBuffer buf,
			final Callback<String, Void> cb) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		checkNotNull("buf", buf); //$NON-NLS-1$
		put_async(ark, key, buf, buf.limit(), cb);
	}

	/*
	 * Gets asynchronously.
	 * 
	 * @param key the key
	 * 
	 * @param buf the buffer to store the result in
	 * 
	 * @param cb the callback
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void get_async(final String key, final ByteBuffer buf,
			final Callback<String, ByteBuffer> cb) throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		get_async(ark, key, buf, cb);
	}

	/*
	 * Deletes asynchronously.
	 * 
	 * @param key the key
	 * 
	 * @param cb the callback
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void delete_async(final String key, final Callback<String, Boolean> cb)
			throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		delete_async(ark, key, cb);
	}

	/*
	 * Asynchronously check if key exists.
	 * 
	 * @param key the key
	 * 
	 * @param cb the callback
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	void contains_key_async(final String key, final Callback<String, Boolean> cb)
			throws IOException {
		checkNotClosed();
		checkNotNull("key", key); //$NON-NLS-1$
		containsKey_async(ark, key, cb);
	}

	/*
	 * get an iterator over the keys.
	 * 
	 * @return the iterator
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	Iterator<String> get_iterator() throws IOException {
		checkNotClosed();
		return get_iterator(ark);
	}

	private static native long create(String path, long flags);

	private static native void close(long ark);

	private static native void put(long ark, String key, ByteBuffer buffer,
			int size);

	private static native boolean get(long ark, String key, ByteBuffer buffer);

	private static native boolean delete(long ark, String key);

	private static native boolean contains_key(long ark, String key);

	private static native void put_async(long ark, String key,
			ByteBuffer buffer, int size, final Callback<String, Void> cb);

	private static native void get_async(long ark, String key,
			ByteBuffer buffer, Callback<String, ByteBuffer> cb);

	private static native void delete_async(long ark, String key,
			Callback<String, Boolean> cb);

	private static native void containsKey_async(long ark, String key,
			Callback<String, Boolean> cb);

	private static native Iterator<String> get_iterator(long ark);

	/**
	 * Key iterator implementation.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 *
	 */
	protected static class KeyIterator implements Iterator<String> {

		/** The ari. */
		private final long ari;

		/** The current key. */
		private String currentKey;

		/**
		 * Instantiates a new key iterator.
		 *
		 * @param ari
		 *            the ari
		 * @param firstKey
		 *            the first key
		 */
		KeyIterator(final long ari, final String firstKey) {
			this.ari = ari;
			currentKey = firstKey;
		}

		/**
		 * {@inheritDoc}
		 */
		// @Override (Java6+)
		public boolean hasNext() {
			return currentKey != null;
		}

		/**
		 * {@inheritDoc}
		 */
		// @Override (Java6+)
		public String next() {
			final String result = currentKey;
			currentKey = next_key(ari);
			return result;
		}

		private static native String next_key(long ari);

		/**
		 * This implementation does not support removal and will throw @see
		 * java.lang.UnsupportedOperationException.
		 */
		public void remove() {
			throw new UnsupportedOperationException("remove"); //$NON-NLS-1$
		}

	}

}
