/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: Example.java $
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
package com.ibm.research.capiblock.example;

import java.io.IOException;
import java.util.concurrent.Future;

import com.ibm.research.capiblock.CapiBlockDevice;
import com.ibm.research.capiblock.Chunk;
import com.ibm.research.capiblock.ObjectStreamBuffer;

/**
 * Example for the capiblock Java library.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 *
 */
public class Example {

        static final String CAPI_DEVICE_PATH_PROP = "CAPI_DEVICE_PATH"; //$NON-NLS-1$ 

	static final String DEVICE = System.getProperty(CAPI_DEVICE_PATH_PROP, "/dev/sdc"); //$NON-NLS-1$
	
	public static void main(final String ... args) throws Exception {
		// some content
		final String message = "this is a message"; //$NON-NLS-1$
		
		// get the capi block device instance
		final CapiBlockDevice dev = CapiBlockDevice.getInstance();
		
		// open a chunk
		try (final Chunk chunk = dev.openChunk(DEVICE)) {
			// helper: wrap the message into a direct buffer
			final ObjectStreamBuffer buf = ObjectStreamBuffer.wrap(message);
			
			// write the buffer to the chunk at position 0, length=1 block
			final long w = chunk.writeBlock(0, 1, buf.getBuffer());
			if (w != 1) {
				throw new IOException("could not write the block"); //$NON-NLS-1$
			}
			
			// do something else
			
			// instantiate a direct buffer for reading one block
			final ObjectStreamBuffer buffer = new ObjectStreamBuffer(1);
			
			// asynchronously read a block from the chunk at position 0
			final Future<Long> f = chunk.readBlockAsync(0, 1, buffer.getBuffer());
			
			// the buffer is not yet valid but we could do something else
			
			// check the future and possibly block until the read has completed and the buffer is valid
			final long r = f.get().longValue();
			if (r != 1) {
				throw new IOException("could not read the block"); //$NON-NLS-1$
			}
			
			final String retrieved = buffer.unwrap();
			System.out.println("Retrieved message: '" + retrieved + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		} 
		
	}
	
}
