/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/ObjectStreamBlockMap.java $
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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ibm.research.blockmap.ObjectStreamBlockMap.Block;

/**
 * The ObjectStreamBlockMap provides developers with a block map facet that uses
 * Java Serialization to read from and write to blocks.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public class ObjectStreamBlockMap extends AbstractBlockMap<Block> {

	/**
	 * Instantiates a new object stream block map.
	 *
	 * @param kvs
	 *            the kvs device
	 */
	public ObjectStreamBlockMap(final CapiKvDevice kvs) {
		super(kvs);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Block createBlock(final int size) {
		return new Block(size);
	}

	/**
	 * The Block implementation for the ObjectStreamBlockMap.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public static final class Block extends AbstractBlockMap.Block {

		/** The (cached) output stream */
		protected ObjectOutputStream out;

		/** The (cached) input stream */
		protected ObjectInputStream in;

		/**
		 * Instantiates a new block.
		 *
		 * @param blockSize
		 *            the block size
		 */
		Block(final int blockSize) {
			super(ByteBuffer.allocateDirect(blockSize));
		}

		/**
		 * Instantiates a new block.
		 *
		 * @param buf
		 *            the buffer to be wrapped
		 */
		Block(final ByteBuffer buf) {
			super(buf);
		}

		/**
		 * Wraps a single object into a Block for the ObjectStreamBlockMap.
		 *
		 * @param map
		 *            the block map.
		 * @param o
		 *            the object to be wrapped.
		 * @return the block.
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public static Block wrap(final ObjectStreamBlockMap map, final int size, final Object o)
				throws IOException {
			final Block block = map.createBlock(size);
			final ObjectOutputStream out = block.getOutputStream();
			out.writeObject(o);
			out.close();
			return block;
		}

		/**
		 * Wraps a series of objects into a Block for the ObjectStreamBlockMap.
		 *
		 * @param map
		 *            the block map.
		 * @param objects
		 *            the objects to be wrapped/
		 * @return the block.
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public static Block wrapAll(final ObjectStreamBlockMap map, final int size,
				final Object... objects) throws IOException {
			final Block block = map.createBlock(size);
			final ObjectOutputStream out = block.getOutputStream();
			for (final Object o : objects) {
				out.writeObject(o);
			}
			out.close();
			return block;
		}

		/**
		 * Unwraps all objects contained in the block. The underlying block is
		 * closed afterwards and subsequent unwrap calls will fail.
		 *
		 * @return the array of objects.
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ClassNotFoundException
		 *             Signals that the class of one or more serialized objects
		 *             could not be found in the class path.
		 */
		public Object[] unwrapMany() throws IOException, ClassNotFoundException {
			final ArrayList<Object> result = new ArrayList<Object>();
			final ObjectInputStream in = getInputStream();

			while (buf.remaining() > 0) {
				result.add(in.readObject());
			}
			in.close();
			return result.toArray();
		}

		/**
		 * Unwraps a single object contained in the block. The underlying block
		 * is closed afterwards and subsequent unwrap calls will fail.
		 *
		 * @param <T>
		 *            the type of the object
		 * @return the t
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 * @throws ClassNotFoundException
		 *             Signals that the class of one or more serialized objects could
		 *             not be found in the class path.
		 */
		public <T> T unwrap() throws IOException, ClassNotFoundException {
			final ObjectInputStream in = getInputStream();
			@SuppressWarnings("unchecked")
			final T t = (T) in.readObject();
			in.close();
			return t;
		}

		/**
		 * Gets the output stream.
		 *
		 * @return the output stream to which applications can write Java
		 *         objects and primitive data in order to fill the block.
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public ObjectOutputStream getOutputStream() throws IOException {
			if (in != null) {
				throw new IllegalStateException("Block is in input mode."); //$NON-NLS-1$
			}
			if (out == null) {
				out = new ObjectOutputStream(new BlockOutputStream());
			}
			return out;
		}

		/**
		 * Gets the input stream.
		 *
		 * @return the input stream from which applications can read JavaObjects
		 *         and primitive data back from a block.
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public ObjectInputStream getInputStream() throws IOException {
			if (out != null) {
				throw new IllegalStateException("Block is in output mode."); //$NON-NLS-1$
			}
			if (in == null) {
				in = new ObjectInputStream(new BlockInputStream());
			}
			return in;
		}

		/**
		 * The Class BlockOutputStream.
		 */
		private final class BlockOutputStream extends OutputStream {

			/**
			 * Instantiates a new block output stream.
			 */
			protected BlockOutputStream() {

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void write(final int b) throws IOException {
				buf.put((byte) b);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void write(final byte b[], final int off, final int len)
					throws IOException {
				buf.put(b, off, len);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void close() throws IOException {
				out = null;
				if (buf.position() > 0) {
					buf.flip();
				}
			}

		}

		/**
		 * The Class BlockInputStream.
		 */
		private final class BlockInputStream extends InputStream {

			/**
			 * Instantiates a new block input stream.
			 */
			protected BlockInputStream() {

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public int read() throws IOException {
				return available() > 0 ? buf.get() : -1;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public int read(final byte[] b, final int off, final int len)
					throws IOException {
				if (len == 0) {
					return 0;
				}

				final int c = Math.min(len, available());

				if (c == 0) {
					return -1;
				}

				buf.get(b, off, len);
				return c;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public int available() {
				return buf.remaining();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public long skip(final long s) {
				final int c = Math.min((int) s, available());
				buf.position(buf.position() + c);
				return c;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void close() throws IOException {
				in = null;

				if (buf.position() > 0) {
					buf.flip();
				}
			}
		}

	}

}
