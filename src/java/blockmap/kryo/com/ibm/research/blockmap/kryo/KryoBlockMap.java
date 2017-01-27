/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/kryo/KryoBlockMap.java $
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
package com.ibm.research.blockmap.kryo;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.ibm.research.blockmap.AbstractBlockMap;
import com.ibm.research.blockmap.CapiKvDevice;
import com.ibm.research.blockmap.kryo.KryoBlockMap.Block;

/**
 * The Class KryoBlockMap.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public class KryoBlockMap extends AbstractBlockMap<Block> {

	/**
	 * Instantiates a new kryo block map.
	 *
	 * @param kvs
	 *            the kvs
	 */
	public KryoBlockMap(final CapiKvDevice kvs) {
		super(kvs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.research.blockmap.AbstractBlockMap#createBlock()
	 */
	@Override
	public Block createBlock(final int size) {
		return new Block(size);
	}

	/**
	 * The Class Block.
	 */
	public static final class Block extends AbstractBlockMap.Block {

		/** The input. */
		protected Input input;

		/** The output. */
		protected Output output;

		/**
		 * Instantiates a new block.
		 *
		 * @param buffer
		 *            the buffer
		 */
		protected Block(final ByteBuffer buffer) {
			super(buffer);
		}

		/**
		 * Instantiates a new block.
		 *
		 * @param blockSize
		 *            the block size
		 */
		protected Block(final int blockSize) {
			super(ByteBuffer.allocateDirect(blockSize));
		}

		/**
		 * Gets the output.
		 *
		 * @return the output
		 */
		public Output getOutput() {
			if (input != null) {
				throw new IllegalStateException("Block is in input mode."); //$NON-NLS-1$
			}
			if (output == null) {
				output = new BlockOutput(buf);
			}

			return output;
		}

		/**
		 * Gets the input.
		 *
		 * @return the input
		 */
		public Input getInput() {
			if (output != null) {
				throw new IllegalStateException("Block is in output mode."); //$NON-NLS-1$
			}
			if (input == null) {
				input = new BlockInput(buf);
			}

			return input;
		}

		/**
		 * The Class BlockInput.
		 */
		private final class BlockInput extends ByteBufferInput {

			/**
			 * Instantiates a new block input.
			 *
			 * @param buf
			 *            the buf
			 */
			protected BlockInput(final ByteBuffer buf) {
				super(buf);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see com.esotericsoftware.kryo.io.ByteBufferInput#close()
			 */
			@Override
			public void close() {
				super.close();

				input = null;
				if (niobuffer.position() > 0) {
					niobuffer.flip();
				}
			}

		}

		/**
		 * The Class BlockOutput.
		 */
		private final class BlockOutput extends ByteBufferOutput {

			/**
			 * Instantiates a new block output.
			 *
			 * @param buf
			 *            the buf
			 */
			protected BlockOutput(final ByteBuffer buf) {
				super(buf);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see com.esotericsoftware.kryo.io.ByteBufferOutput#close()
			 */
			@Override
			public void close() {
				super.close();

				output = null;
				if (niobuffer.position() == 0) {
					niobuffer.flip();
				}
			}
		}

	}

}
