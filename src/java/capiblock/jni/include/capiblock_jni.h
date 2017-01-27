/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_ibm_research_capiblock_CapiBlockDevice */

#ifndef _Included_com_ibm_research_capiblock_CapiBlockDevice
#define _Included_com_ibm_research_capiblock_CapiBlockDevice
#ifdef __cplusplus
extern "C" {
#endif
#undef com_ibm_research_capiblock_CapiBlockDevice_BLOCK_SIZE
#define com_ibm_research_capiblock_CapiBlockDevice_BLOCK_SIZE 4096L
/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    open
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_open
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_close
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    get_lun_size
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_get_1lun_1size
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    read
 * Signature: (IJJLjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_read
  (JNIEnv *, jobject, jint, jlong, jlong, jobject);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    write
 * Signature: (IJJLjava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_write
  (JNIEnv *, jobject, jint, jlong, jlong, jobject);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    readAsync
 * Signature: (IJJLjava/nio/ByteBuffer;)Lcom/ibm/research/capiblock/Chunk/CapiBlockFuture;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_readAsync
  (JNIEnv *, jobject, jint, jlong, jlong, jobject);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    writeAsync
 * Signature: (IJJLjava/nio/ByteBuffer;)Lcom/ibm/research/capiblock/Chunk/CapiBlockFuture;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_writeAsync
  (JNIEnv *, jobject, jint, jlong, jlong, jobject);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    get_stats
 * Signature: (I)Lcom/ibm/research/capiblock/Stats;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_get_1stats
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_ibm_research_capiblock_CapiBlockDevice
 * Method:    releaseMemory
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_capiblock_CapiBlockDevice_releaseMemory
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif