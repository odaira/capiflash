/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/BlockMap.java $
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

import java.nio.ByteBuffer;

/**
 * The BlockMap contains support for bare blocks which are created from (direct)
 * {@link java.nio.ByteBuffer}.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public class BlockMap extends AbstractBlockMap<BlockMap.Block> {

	/**
	 * Instantiates a new block map.
	 *
	 * @param kvs
	 *            the kvs device.
	 */
	public BlockMap(final CapiKvDevice kvs) {
		super(kvs);
	}

	public Block createBlock(final ByteBuffer buf) {
		return new Block(buf);
	}

	@Override
	public Block createBlock(int size) {
		return new Block(ByteBuffer.allocateDirect(size));
	}

	/**
	 * The Block implementation for BlockMap.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public static final class Block extends AbstractBlockMap.Block {

		/**
		 * Instantiates a new block.
		 *
		 * @param buffer
		 *            the buffer to be wrapped
		 */
		protected Block(final ByteBuffer buffer) {
			super(buffer);
		}

		/**
		 * Gets the wrapped buffer.
		 *
		 * @return the buffer
		 */
		public ByteBuffer getBuffer() {
			return buf;
		}

	}

}
