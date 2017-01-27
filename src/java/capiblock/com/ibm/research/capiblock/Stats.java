/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/com/ibm/research/capiblock/Stats.java $
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

/**
 * A POJO exposing statistics about the chunk.
 * 
 * @author Jan S. Rellermeyer, IBM Research
 */
public class Stats {

	/** Block size of this chunk. */
	public final int blockSize;
	/** Number of paths of this chunk. */
	public final int numPaths;
	/** Maximum transfer size in blocks of this chunk. */
	public final long maxTransferSize;
	/** Total number of synchronous reads issued */
	public final long numReads;
	/** Total number of synchronous writes issued */
	public final long numWrites;
	/** Total number of asynchronous reads issued */
	public final long numAReads;
	/** Total number of async writes issues    */
	public final long numAWrites;
	/** Current number of synchronous reads active  */
	public final long numActReads;
	/** Current number of synchronous writes active  */
	public final long numActWrites;
	/** Current number of asynchronous reads active  */
	public final long numActAReads;
	/** Current number of asynchronous writes active  */
	public final long numActAWrites;
	/** High water mark on the maximum number of synchronous writes active at once */
	public final long maxNumActWrites;
	/** High water mark on the maximum number of synchronous reads active at once */
	public final long maxNumActReads;
	/** High water mark on the maximum number of asynchronous writes active at once */
	public final long maxNumActAWrites;
	/** High water mark on the maximum number of asynchronous reads active at once */
	public final long maxNumActAReads;
	/** Total number of blocks read */
	public final long numBlocksRead;
	/** Total number of blocks written */
	public final long numBlocksWritten;
	/** Total number of all error responses seen */
	public final long numErrors;
	/** Number of times cblk_aresult returned with no command completion */
	public final long numAresultNoCmplt;
	/** Total number of all command retries. */
	public final long numRetries;
	/** Total number of all command time-outs. */
	public final long numTimeouts;
	/** Total number of all command time-outs that led to a command failure. */
	public final long numFailTimeouts;
	/** Total number of times we did not have free command available */
	public final long numNoCmdsFree;
	/** Total number of times we didn't have room to issue a command to the AFU. */
	public final long numNoCmdRoom;
	/**
	 * Total number of times we didn't have free command available and failed a
	 * request because of this
	 */
	public final long numNoCmdsFreeFail;
	/** Total number of all FC error responses seen */
	public final long numFcErrors;
	/** Total number of all link downs seen on port 0. */
	public final long numPort0Linkdowns;
	/** Total number of all link downs seen on port 1. */
	public final long numPort1Linkdowns;
	/** Total number of all no logins seen on port 0. */
	public final long numPort0NoLogins;
	/** Total number of all no logins seen on port 1. */
	public final long numPort1NoLogins;
	/** Total number of all general FC errors seen on port 0. */
	public final long numPort0FcErrors;
	/** Total number of all general FC errors seen on port 1. */
	public final long numPort1FcErrors;
	/** Total number of all check condition responses seen. */
	public final long numCcErrors;
	/** Total number of all AFU error responses seen. */
	public final long numAfuErrors;
	/**
	 * Total number of all times poll indicated a read was ready but there was
	 * nothing to read.
	 */
	public final long numCapiFalseReads;
	/** Total number of all adapter reset errors. */
	public final long numCapiReadFails;
	/** Total number of all CXL_EVENT_READ_FAIL responses seen. */
	public final long numCapiAdapResets;
	/** Total number of all check adapter errors. */
	public final long numCapiAdapChckErr;
	/** Total number of all CXL_EVENT_RESERVED responses seen. */
	public final long numCapiReservedErrs;
	/** Total number of all CAPI data storage event responses seen. */
	public final long numCapiDataStErrs;
	/** Total number of all CAPI error responses seen. */
	public final long numCapiAfuErrors;
	/** Total number of all CAPI AFU interrupts for command responses seen. */
	public final long numCapiAfuIntrpts;
	/** Total number of all of unexpected AFU interrupts. */
	public final long numCapiUnexpAfuIntrpts;
	/** Total number of pthread_creates that succeed. */
	public final long numSuccessThreads;
	/** Total number of pthread_creates that failed. */
	public final long numFailedThreads;
	/** Number of threads we had to cancel, which succeeded. */
	public final long numCancThreads;
	/** Number of threads we had to cancel, but the cancel failed. */
	public final long numFailCancThreads;
	/** Number of threads we detached but the detach failed. */
	public final long numFailDetachThreads;
	/** Current number of threads running. */
	public final long numActiveThreads;
	/** Maximum number of threads running simultaneously. */
	public final long maxNumActThreads;
	/** Total number of cache hits seen on all reads. */
	public final long numCacheHits;
	
	protected Stats(final int blockSize, final int numPaths,
			final long maxTransferSize, final long numReads,
			final long numWrites, final long numAReads, final long numAWrites,
			final long numActReads, final long numActWrites,
			final long numActAReads, final long numActAWrites,
			final long maxNumActReads, final long maxNumActWrites,
			final long maxNumActAReads, final long maxNumActAWrites,
			final long numBlocksRead, final long numBlocksWritten,
			final long numErrors, final long numAresultNoCmplt,
			final long numRetries, final long numTimeouts,
			final long numFailTimeouts, final long numNoCmdsFree,
			final long numNoCmdRoom, final long numNoCmdsFreeFail,
			final long numFcErrors, final long numPort0Linkdowns,
			final long numPort1Linkdowns, final long numPort0NoLogins,
			final long numPort1NoLogins, final long numPort0FcErrors,
			final long numPort1FcErrors, final long numCcErrors,
			final long numAfuErrors, final long numCapiFalseReads,
			final long numCapiReadFails, final long numCapiAdapResets,
			final long numCapiAdapChckErr, final long numCapiReservedErrs,
			final long numCapiDataStErrs, final long numCapiAfuErrors,
			final long numCapiAfuIntrpts, final long numCapiUnexpAfuIntrpts,
			final long numSuccessThreads, final long numFailedThreads,
			final long numCancThreads, final long numFailCancThreads,
			final long numFailDetachThreads, final long numActiveThreads,
			final long maxNumActThreads, final long numCacheHits) {
		this.blockSize = blockSize;
		this.numPaths = numPaths;
		this.maxTransferSize = maxTransferSize;
		this.numReads = numReads;
		this.numWrites = numWrites;
		this.numAReads = numAReads;
		this.numAWrites = numAWrites;
		this.numActReads = numActReads;
		this.numActWrites = numActWrites;
		this.numActAReads = numActAReads;
		this.numActAWrites = numActAWrites;
		this.maxNumActReads = maxNumActReads;
		this.maxNumActWrites = maxNumActWrites;
		this.maxNumActAReads = maxNumActAReads;
		this.maxNumActAWrites = maxNumActAWrites;
		this.numBlocksRead = numBlocksRead;
		this.numBlocksWritten = numBlocksWritten;
		this.numErrors = numErrors;
		this.numAresultNoCmplt = numAresultNoCmplt;
		this.numRetries = numRetries;
		this.numTimeouts = numTimeouts;
		this.numFailTimeouts = numFailTimeouts;
		this.numNoCmdsFree = numNoCmdsFree;
		this.numNoCmdRoom = numNoCmdRoom;
		this.numNoCmdsFreeFail = numNoCmdsFreeFail;
		this.numFcErrors = numFcErrors;
		this.numPort0Linkdowns = numPort0Linkdowns;
		this.numPort1Linkdowns = numPort1Linkdowns;
		this.numPort0NoLogins = numPort0NoLogins;
		this.numPort1NoLogins = numPort1NoLogins;
		this.numPort0FcErrors = numPort0FcErrors;
		this.numPort1FcErrors = numPort1FcErrors;
		this.numCcErrors = numCcErrors;
		this.numAfuErrors = numAfuErrors;
		this.numCapiFalseReads = numCapiFalseReads;
		this.numCapiReadFails = numCapiReadFails;
		this.numCapiAdapResets = numCapiAdapResets;
		this.numCapiAdapChckErr = numCapiAdapChckErr;
		this.numCapiReservedErrs = numCapiReservedErrs;
		this.numCapiDataStErrs = numCapiDataStErrs;
		this.numCapiAfuErrors = numCapiAfuErrors;
		this.numCapiAfuIntrpts = numCapiAfuIntrpts;
		this.numCapiUnexpAfuIntrpts = numCapiUnexpAfuIntrpts;
		this.numSuccessThreads = numSuccessThreads;
		this.numFailedThreads = numFailedThreads;
		this.numCancThreads = numCancThreads;
		this.numFailCancThreads = numFailCancThreads;
		this.numFailDetachThreads = numFailDetachThreads;
		this.numActiveThreads = numActiveThreads;
		this.maxNumActThreads = maxNumActThreads;
		this.numCacheHits = numCacheHits;
	}

}