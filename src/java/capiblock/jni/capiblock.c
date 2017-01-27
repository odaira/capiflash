/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/jni/capiblock.c $
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

// @author Jan S. Rellermeyer, IBM Research

#define TRACE_ENABLE 0

#include <stddef.h>
#include <stdlib.h>
#include <jni.h>
#include <fcntl.h>
#include <assert.h>
#include <string.h>
#include <errno.h>

#include <capiblock_lib.h>
#include <capiblock_ex.h>
#include <capiblock_jni.h>

#include <capiblock.h>

#include <sys/types.h>
#include <unistd.h>

#define BLOCK_SIZE 4096

static jclass future_cls = NULL;
static jmethodID future_constr = 0;

static jclass stat_cls = NULL;
static jmethodID stat_constr = 0;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
   UNUSED(vm)
   UNUSED(reserved)

   int rc = cblk_init(NULL, 0);
   assert(rc == 0 && "cblk_init");

   return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
   UNUSED(vm)
   UNUSED(reserved)

   JNIEnv* env;
   int rc = (*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_2);
   if (rc == 0)
   {
      if (future_cls != NULL)
      {
         uncache_class(env, future_cls);
      }
      if (stat_cls != NULL)
      {
         uncache_class(env, stat_cls);
      }
   }

   cblk_term(NULL, 0);
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    open
 * Signature: (Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_open(
      JNIEnv *env, jobject obj, jstring path, jint max_num_requests)
{
   UNUSED(obj)

   if (max_num_requests < 0)
   {
      goto ioerror2;
   }

   const char* device_path;
   if (path == NULL)
   {
      device_path = NULL;
   }
   else
   {
      device_path = (*env)->GetStringUTFChars(env, path, NULL);
      if (device_path == NULL)
      {
         throwOOMError(env, "Could not retrieve device_path.");
         return -1;
      }
   }

   chunk_id_t chunk = cblk_open(device_path, max_num_requests, O_RDWR, 0, 0);

   TRACE("opened chunk at %s, chunk id=%d\n", device_path, (jint) chunk);

   if (device_path != NULL)
   {
      (*env)->ReleaseStringUTFChars(env, path, device_path);
   }

   if (chunk == NULL_CHUNK_ID)
   {
      goto ioerror;
   }

   return (jint) chunk;
   ioerror: throwIOExceptionE(env, "Could not open chunk", errno);
   return -1;
   ioerror2: throwIOException(env, "Invalid option");
   return -1;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_close(
      JNIEnv *env, jobject obj, jint chunk_id)
{
   UNUSED(obj)

   int rc = cblk_close((chunk_id_t) chunk_id, 0);

   if (rc == 0)
   {
      TRACE("closed chunk %d\n", chunk_id);
      return;
   }

   throwIOExceptionE(env, "Could not close chunk", errno);
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    get_lun_size
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_get_1lun_1size(
      JNIEnv *env, jobject obj, jint chunk_id)
{
   UNUSED(env)
   UNUSED(obj)

   size_t size;

   int rc = cblk_get_lun_size(chunk_id, &size, 0);
   if (rc != 0)
   {
      goto ioerror;
   }

   return size;

   ioerror: throwIOExceptionE(env, "Could not get lun size", errno);
   return -1;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    read
 * Signature: (IJJLjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_read(
      JNIEnv *env, jobject obj, jint chunk_id, jlong lba, jlong nblocks, jobject buf)
{
   UNUSED(obj)

   void* buffer = (*env)->GetDirectBufferAddress(env, buf);
   //assert(((unsigned long )buf & 15) == 0
   //            && "buffer address is not 16-byte aligned");

   int rc = cblk_read((chunk_id_t) chunk_id, buffer, lba, nblocks, 0);

   if (rc < 0)
   {
      goto ioerror;
   }

   return rc;

   ioerror: throwIOExceptionE(env, "Could not read from chunk", errno);
   return -1;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    write
 * Signature: (IJJLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_write(
      JNIEnv *env, jobject obj, jint chunk_id, jlong lba, jlong nblocks,
      jobject buffer)
{
   UNUSED(obj)

   void* buf = (*env)->GetDirectBufferAddress(env, buffer);

   //assert(
   //      ((unsigned long )buf & 15) == 0
   //            && "buffer address is not 16-byte aligned");

   int rc = cblk_write((chunk_id_t) chunk_id, buf, lba, nblocks, 0);
   if (rc < 0)
   {
      goto ioerror;
   }

   return rc;

   ioerror: throwIOExceptionE(env, "Could not write to chunk", errno);
   return -1;
}

jobject create_future(JNIEnv *env, cblk_arw_status_t * status)
{
   if (future_cls == NULL)
   {
      cache_class(env, future_cls,
            "com/ibm/research/capiblock/Chunk$CapiBlockFuture",
            {
               future_constr = (*env)->GetMethodID(env, future_cls, "<init>",
                     "(JJJ)V")
               ;
            }, return NULL;);
   }

   TRACE("ADDRESS OF STATUS: %p\n", (void*) status);
   TRACE("TRANSMITTED: %lu\n",  ((uint64_t) status) + offsetof(cblk_arw_status_t, status));
   TRACE("BLOCKS_TRANSFERRED: %lu\n",  ((uint64_t) status) + offsetof(cblk_arw_status_t, blocks_transferred));

   jobject future = (*env)->NewObject(env, future_cls, future_constr,
         ((uint64_t) status) + offsetof(cblk_arw_status_t, status),
         ((uintptr_t) status) + offsetof(cblk_arw_status_t, blocks_transferred),
         ((uintptr_t) status) + offsetof(cblk_arw_status_t, fail_errno));

   return future;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    readAsync
 * Signature: (IJJLjava/nio/ByteBuffer;)Lcom/ibm/research/capiblock/Chunk/CapiBlockFuture;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_readAsync(
      JNIEnv *env, jobject obj, jint chunk_id, jlong lba, jlong nBlocks,
      jobject buf)
{
   UNUSED(obj)
   cblk_arw_status_t *status = (cblk_arw_status_t *) malloc(
         sizeof(cblk_arw_status_t));
   bzero(status, sizeof(cblk_arw_status_t));

   void* buffer = (*env)->GetDirectBufferAddress(env, buf);
   int tag;

   int rc = cblk_aread((chunk_id_t) chunk_id, buffer, lba, nBlocks, &tag,
         status,
         CBLK_ARW_WAIT_CMD_FLAGS | CBLK_ARW_USER_STATUS_FLAG);
   if (rc < 0)
   {
      TRACE("error: cblk_aread returned code %d\n", rc);
      goto ioerror;
   }

   return create_future(env, status);

   ioerror: throwIOExceptionE(env,
         "Could not enqueue asynchronous read request", errno);
   return NULL;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    writeAsync
 * Signature: (IJJLjava/nio/ByteBuffer;)Lcom/ibm/research/capiblock/Chunk/CapiBlockFuture;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_writeAsync(
      JNIEnv *env, jobject obj, jint chunk_id, jlong lba, jlong nBlocks,
      jobject buf)
{
   UNUSED(obj)
   cblk_arw_status_t *status = (cblk_arw_status_t *) malloc(
         sizeof(cblk_arw_status_t));
   bzero(status, sizeof(cblk_arw_status_t));

   void* buffer = (*env)->GetDirectBufferAddress(env, buf);
   int tag;

   int rc = cblk_awrite((chunk_id_t) chunk_id, buffer, lba, nBlocks, &tag,
         status,
         CBLK_ARW_WAIT_CMD_FLAGS | CBLK_ARW_USER_STATUS_FLAG);
   if (rc < 0)
   {
      TRACE("error: cblk_awrite returned code %d\n", rc);
      goto ioerror;
   }

   return create_future(env, status);

   ioerror: throwIOExceptionE(env,
         "Could not enqueue asynchronous write request", errno);
   return NULL;
}

jobject create_stat(JNIEnv* env, chunk_stats_t* stat)
{
   if (stat_cls == NULL)
   {
      cache_class(env, stat_cls,
            "com/ibm/research/capiblock/Stats",
            {
               stat_constr = (*env)->GetMethodID(env, stat_cls, "<init>",
                     "(IIJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ)V");
            }, return NULL;);
   }

   jobject stats = (*env)->NewObject(env, stat_cls, stat_constr,
         stat->block_size, // final int blockSize
         stat->num_paths, // final int numPaths
         stat->max_transfer_size, // final long maxTransferSize
         stat->num_reads, //  final long numReads
         stat->num_writes, // final long numWrites
         stat->num_areads, // final long numAReads
         stat->num_awrites, // final long numAWrites
         stat->num_act_reads,  // final long numActReads,
         stat->num_act_writes, // final long numActWrites,
         stat->num_act_areads, // final long numActAReads,
         stat->num_act_awrites, // final long numActAWrites,
         stat->max_num_act_reads, // final long maxNumActReads,
         stat->max_num_act_writes, // final long maxNumActWrites,
         stat->max_num_act_areads, // final long maxNumActAReads,
         stat->max_num_act_awrites, // final long maxNumActAWrites,
         stat->num_blocks_read, // final long numBlocksRead,
         stat->num_blocks_written, // final long numBlocksWritten,
         stat->num_errors, // final long numErrors,
         stat->num_aresult_no_cmplt, // final long numAresultNoCmplt,
         stat->num_retries, // final long numRetries,
         stat->num_timeouts, // final long numTimeouts,
         stat->num_fail_timeouts, // final long numFailTimeouts,
         stat->num_no_cmds_free, // final long numNoCmdsFree,
         stat->num_no_cmd_room, // final long numNoCmdRoom,
         stat->num_no_cmds_free_fail, // final long numNoCmdsFreeFail,
         stat->num_fc_errors, // final long numFcErrors,
         stat->num_port0_linkdowns, // final long numPort0Linkdowns,
         stat->num_port1_linkdowns, // final long numPort1Linkdowns,
         stat->num_port0_no_logins, // final long numPort0NoLogins,
         stat->num_port1_no_logins, // final long numPort1NoLogins,
         stat->num_port0_fc_errors, // final long numPort0FcErrors,
         stat->num_port1_fc_errors, // final long numPort1FcErrors,
         stat->num_cc_errors, // final long numCcErrors,
         stat->num_afu_errors, // final long numAfuErrors,
         stat->num_capi_false_reads, // final long numCapiFalseReads,
         stat->num_capi_read_fails, // final long numCapiReadFails,
         stat->num_capi_adap_resets, // final long numCapiAdapResets,
         stat->num_capi_adap_chck_err, // final long numCapiAdapChckErr,
         stat->num_capi_reserved_errs, // final long numCapiReservedErrs,
         stat->num_capi_data_st_errs, // final long numCapiDataStErrs,
         stat->num_capi_afu_errors, // final long numCapiAfuErrors,
         stat->num_capi_afu_intrpts, // final long numCapiAfuIntrpts,
         stat->num_capi_unexp_afu_intrpts, // final long numCapiUnexpAfuIntrpts,
         stat->num_success_threads, // final long numSuccessThreads,
         stat->num_failed_threads, // final long numFailedThreads,
         stat->num_canc_threads, // final long numCancThreads,
         stat->num_fail_canc_threads, // final long numFailCancThreads,
         stat->num_fail_detach_threads, // final long numFailDetachThreads,
         stat->num_active_threads, // final long numActiveThreads,
         stat->max_num_act_threads, // final long maxNumActThreads,
         stat->num_cache_hits //final long numCacheHits
   );

   return stats;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    get_stats
 * Signature: (I)Lcom/ibm/research/capiblock/Chunk/Stats;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_get_1stats
  (JNIEnv *env, jobject obj, jint chunk_id)
{
   UNUSED(obj)

   chunk_stats_t stat;

   int rc = cblk_get_stats((chunk_id_t) chunk_id, &stat, 0);
   if (rc != 0)
   {
      goto ioerror;
   }

   return create_stat(env, &stat);

   ioerror: throwIOExceptionE(env, "Could not get write to chunk", errno);
   return NULL;
}

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    releaseMemory
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_releaseMemory
  (JNIEnv *env, jclass cls, jlong addr)
{
   UNUSED(env)
   UNUSED(cls)

   // free the memory previously assigned to a status struct
   free((void*) addr);
}
