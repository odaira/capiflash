/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/jni/blockmap.c $
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

#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <errno.h>

#include "jni.h"

#include "blockmap.h"
#include "blockmap_jni.h"
#include "blockmap_ex.h"

#include "arkdb.h"

// internally use the asynchronous ark API for synchronous calls?
#define USE_ASYNC 0

// maximum length of a key
#define KEY_LENGTH 4096

// number of concurrent asynchronous requests
#define NASYNC 128

#define VOID_RESULT (NULL)
#define BOOL_RESULT ((void*) -1)

// TLS key for the arc
static pthread_key_t arc_key = 0;

// TLS key to abuse the destructor for detaching
// thread from the JVM to avoid a resource leak
static pthread_key_t detach_key = 0;

// cached class objects and method id
static jclass callback_cls = NULL;
static jmethodID method_pos;
static jmethodID method_neg;

static jclass byte_buffer_cls = NULL;
static jmethodID method_limit;

static jclass exception_cls = NULL;
static jmethodID method_constr;

static jobject true_obj = NULL;
static jobject false_obj = NULL;

static jclass key_iterator_cls = NULL;
static jmethodID key_iterator_constr;

JavaVM *jvm;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
	UNUSED(reserved)
	jvm = vm;
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
	UNUSED(reserved)
	JNIEnv *env;
	(*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_2);

	uncache_class(env, byte_buffer_cls);
	uncache_class(env, exception_cls);
	uncache_class(env, callback_cls);
	uncache_class(env, true_obj);
	uncache_class(env, false_obj);
	uncache_class(env, key_iterator_cls);
}

static inline jobject create_exception_object(JNIEnv *env, const char* msg)
{
	cache_class(env, exception_cls, "java/io/IOException", {
			method_constr = (*env)->GetMethodID(env, exception_cls, "<init>", "(Ljava/lang/String;)V");
	}, return NULL);

	jstring msg_str = (*env)->NewStringUTF(env, msg);
	if (msg_str == NULL)
	{
		throwOOMError(env, "Could not allocate msg_str");
		return NULL;
	}

	jobject result = (*env)->NewObject(env, exception_cls, method_constr,
			msg_str);

	return result;
}

static inline ARC* ensure_connected(jlong ark)
{
	ARC* arc;
	if ((arc = pthread_getspecific(arc_key)) == NULL)
	{
		ark_connect_verbose(&arc, (ARK*) ark, NASYNC);
		TRACE("connected thread to ark, arc=%p\n", (void*) arc);
		pthread_setspecific(arc_key, arc);
	}

	return arc;
}

/*
 * TLS destructor function
 */
static void disconnect(void *arg)
{
	ARC* arc = (ARC*) arg;
	int rc = ark_disconnect(arc);
	assert(rc == 0);
}

/*
 * thread detaching function
 * used as a TLS destructor
 */
static void detach(void *arg)
{
	UNUSED(arg);
	assert(jvm != NULL);

	TRACE("Detaching the current thread from the JVM\n");

	(*jvm)->DetachCurrentThread(jvm);
}

static inline void call_callback_object(JNIEnv *env, jobject callback,
		jboolean success, jobject arg1, jobject arg2)
{
	cache_class(env, callback_cls, "com/ibm/research/blockmap/Callback",
			{
				method_pos = (*env)->GetMethodID(env, callback_cls,
						"operationCompleted",
						"(Ljava/lang/Object;Ljava/lang/Object;)V")
				;
				method_neg = (*env)->GetMethodID(env, callback_cls,
						"operationFailed",
						"(Ljava/lang/Object;Ljava/lang/Exception;)V")
				;
			}, return);

	TRACE("thread %lu calling the callback method (%s) with env %p and method_id %lu\n", pthread_self(), (success ? "SUCCESS" : "FAILURE"), (void* ) env, (uint64_t ) ((void* ) (success ? method_pos : method_neg)));

	(*env)->CallVoidMethod(env, callback, (success ? method_pos : method_neg),
			arg1, arg2);

	TRACE("done with the callback method, exceptions: %d\n", (*env)->ExceptionCheck(env));

	// clear any exception that might have occurred in the callback
	if ((*env)->ExceptionOccurred(env))
	{
#if TRACE_ENABLE
		(*env)->ExceptionDescribe(env);
#endif
		(*env)->ExceptionClear(env);
	}
}

static inline void set_limit(JNIEnv *env, jobject buf, jint limit)
{
	cache_class(env, byte_buffer_cls, "java/nio/ByteBuffer", {
			method_limit = (*env)->GetMethodID(env, byte_buffer_cls, "limit", "(I)Ljava/nio/Buffer;");
	}, return);

	(*env)->CallObjectMethod(env, buf, method_limit, limit);
}

static inline void cache_boolean_objs(JNIEnv *env) {
	if (true_obj == NULL)
	{
		jclass _boolean_cls = (*env)->FindClass(env, "java/lang/Boolean");
		if (_boolean_cls == NULL)
		{
			throwIllegalStateException(env, "Could not find Boolean class");
			return;
		}

		jmethodID _boolean_constr = (*env)->GetMethodID(env, _boolean_cls,
				"<init>", "(Z)V");
		assert(_boolean_constr != 0);

		jobject _true_obj = (*env)->NewObject(env, _boolean_cls, _boolean_constr,
		JNI_TRUE);
		assert(_true_obj != NULL);
		true_obj = (*env)->NewGlobalRef(env, _true_obj);

		jobject _false_obj = (*env)->NewObject(env, _boolean_cls, _boolean_constr,
		JNI_FALSE);
		assert(_false_obj != NULL);
		false_obj = (*env)->NewGlobalRef(env, _false_obj);
	}
}

#if (USE_ASYNC)

typedef struct
{
	int errcode;
	int64_t res;
	volatile uint64_t condvar;
}cb_info __attribute__ ((aligned(16)));

static void callback(int errcode, uint64_t dt, int64_t res)
{
	cb_info *s = (cb_info*) dt;

	assert(s->condvar == 0);

	TRACE("received callback, errcode=%d, res=%ld, condvar=%lu\n", errcode, res, s->condvar);

	s->errcode = errcode;
	s->res = res;
	s->condvar = 1;
}

#endif

static void call_java_callback(int errcode, uint64_t dt, int64_t res)
{
	JNIEnv *env;

	assert(jvm != NULL);
	assert(detach_key != 0);

	TRACE("Thread %lu callback chain\n", pthread_self());

	assert(dt != 0);
	cb_tag *tag = (cb_tag*) dt;

	int rc = (*jvm)->AttachCurrentThreadAsDaemon(jvm, (void**) &env, NULL);
	assert(rc >= 0);

	if (pthread_getspecific(detach_key) == NULL)
	{
		TRACE("Attaching current thread %lu to the JVM\n", pthread_self());

		pthread_setspecific(detach_key, (void*) 1);
	}

	if (tag->callback_object == NULL) goto cleanup1;

	if (errcode != 0 && errcode != ENOENT)
	{
		char msg[MSG_LEN];
		snprintf(msg, MSG_LEN, "Error code: %d", errcode);

		jobject ex = create_exception_object(env, msg);
		if (ex == NULL)
		{
			// generated an exception
			goto cleanup;
		}

		call_callback_object(env, tag->callback_object, JNI_FALSE, tag->key, ex);

		goto cleanup;
	}

	TRACE("Received callback: errcode=%d, res=%ld, i->result=%p\n", errcode,
			res, tag->result);

	jobject result = NULL;
	if (tag->result == VOID_RESULT)
	{
		result = NULL;
	}
	else if (tag->result == BOOL_RESULT)
	{
		cache_boolean_objs(env);

		result = (errcode == ENOENT) ? false_obj : true_obj;
	}
	else
	{
		result = tag->result;

		if (errcode == ENOENT) {
			set_limit(env, result, 0);
		} else {
			int64_t limit = (*env)->GetDirectBufferCapacity(env, result);
			set_limit(env, result, limit < res ? limit : res);
		}
	}

	TRACE("about to call callback object with %p and %p\n", (void*) tag->key, (void*) result);

	call_callback_object(env, tag->callback_object, JNI_TRUE, tag->key, result);

cleanup:
	// remove the global references
	if (tag->callback_object != NULL)
	{
		(*env)->DeleteGlobalRef(env, tag->callback_object);
	}
cleanup1:
	if (((int64_t)tag->result) > 0)
	{
		(*env)->DeleteGlobalRef(env, tag->result);
	}
	if (tag->key_chars != NULL)
	{
		(*env)->ReleaseStringUTFChars(env, tag->key, tag->key_chars);
	}
	if (tag->key != NULL)
	{
		(*env)->DeleteGlobalRef(env, tag->key);
	}

	free(tag);
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    create
 * Signature: (Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_create(
		JNIEnv *env, jclass cls, jstring path, jlong flags)
{
	UNUSED(cls);
	ARK *ark;

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

	//int rc = ark_create_verbose((char*) device_path, &ark, 1048576, block_size,
	//		16, 256, 34, 1024, 0, 0);
	//int rc = ark_create_verbose((char*) device_path, &ark, (4294967296 / block_size), block_size,
	//		(4294967296 / block_size), 102400, 34, 102400, 40, 128000, flags);
	//int rc = ark_create_verbose((char*) device_path, &ark, 1048576, 4096,
	//			16, 256, 34, 1024, 0, 0, flags);
	int rc = ark_create((char*) device_path, &ark, flags);

	if (device_path != NULL) {
		(*env)->ReleaseStringUTFChars(env, path, device_path);
	}

	if (rc != 0)
	{
		throwIOExceptionE(env, "ark_create_verbose", rc);
		return rc;
	}

	TRACE("ark_create_verbose created ARK %p\n", (void* ) ark);

	// create key for arc TLS
	pthread_key_create(&arc_key, disconnect);

	// create key for detach TLS
	pthread_key_create(&detach_key, detach);

	return (jlong) ark;
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_close(
		JNIEnv *env, jclass cls, jlong ark)
{
	UNUSED(cls);

	// delete key for TLS
	pthread_key_delete(arc_key);

	int rc = ark_delete((ARK*) ark);
	if (rc != 0)
	{
		throwIOExceptionE(env, "ark_delete", rc);
	}
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    put
 * Signature: (JLjava/lang/String;Ljava/nio/ByteBuffer;I)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_put(JNIEnv *env,
		jclass cls, jlong ark, jstring key, jobject value, jint size)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	void* buf = (*env)->GetDirectBufferAddress(env, value);
	if (buf == NULL)
	{
		throwIllegalStateException(env, "direct buffer address was NULL");
		return;
	}

	int64_t res = 0;

#if (USE_ASYNC)
	cb_info s;
	memset(&s, 0, sizeof(cb_info));

	TRACE("before ark_set_async_cb, key_length=%lu, key_chars=%p, key='%s' size=%u, buf=%p, dt=%lu\n", key_length, (void*) key_chars, key_chars, size, buf, (uint64_t) &s);

	int rc = ark_set_async_cb(ensure_connected(ark), key_length, (void*) key_chars, size, buf, &callback,
			(uint64_t) &s);

	if (rc != 0)
	{
		throwIOExceptionE(env, "Could not enqueue set command", rc);
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		return;
	}

	while (!s.condvar)
	{
		// spin
	}

	rc = s.errcode;
	res = s.res;
#else
	TRACE("before ark_set, key_length=%lu, key_chars=%p, size=%u, buf=%p\n",
			key_length, (void* ) key_chars, size, buf);

	int rc = ark_set((ARK*) ark, key_length, (void*) key_chars, size, buf,
			&res);
#endif
	(*env)->ReleaseStringUTFChars(env, key, key_chars);

	if (rc != 0)
	{
		throwIOExceptionE(env, "Could not enqueue set command", rc);
		return;
	}

	if (res != size)
	{
		throwIOException(env, "put returned short write");
	}
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    get
 * Signature: (JLjava/lang/String;Ljava/nio/ByteBuffer;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_get(
		JNIEnv *env, jclass cls, jlong ark, jstring key, jobject buffer)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	void* buf = (*env)->GetDirectBufferAddress(env, buffer);
	if (buf == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOException(env, "Could not access buffer");
		return JNI_FALSE;
	}

	const jlong buf_size = (*env)->GetDirectBufferCapacity(env, buffer);
	if (buf_size < 0) {
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOException(env, "Invalid buffer");
		return JNI_FALSE;
	}

	int64_t res;

#if (USE_ASYNC)
	cb_info s;
	memset(&s, 0, sizeof(cb_info));

	int rc = ark_get_async_cb(ensure_connected(ark), key_length, (void*) key_chars, buf_size, buf, 0,
			&callback, (uint64_t) &s);

	if (rc != 0)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOExceptionE(env, "Could not enqueue get command", rc);

		set_limit(env, buffer, 0);

		return JNI_FALSE;
	}

	while (!s.condvar)
	{
		// spin
	}

	rc = s.errcode;
	res = s.res;
#else
	TRACE("before ark_get, key_length=%lu, key_chars=%p\n", key_length,
			(void* ) key_chars);

	int rc = ark_get((ARK*) ark, key_length, (void*) key_chars, buf_size, buf,
			0, &res);
#endif
	(*env)->ReleaseStringUTFChars(env, key, key_chars);

	if (rc == 0)
	{
		set_limit(env, buffer, buf_size < res ? buf_size : res);

		return JNI_TRUE;
	}

	if (rc == ENOENT)
	{
		set_limit(env, buffer, 0);

		// there is no such key
		return JNI_FALSE;
	}

	throwIOExceptionE(env, "Could not enqueue get command", rc);
	return JNI_FALSE;
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    delete
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_delete(
		JNIEnv *env, jclass cls, jlong ark, jstring key)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return JNI_FALSE;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

#if (USE_ASYNC)
	cb_info s;
	memset(&s, 0, sizeof(cb_info));

	int rc = ark_del_async_cb(ensure_connected(ark), key_length, (void*) key_chars, &callback,
			(uint64_t) &s);

	if (rc != 0)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOExceptionE(env, "Could not enqueue delete command", rc);

		return JNI_FALSE;
	}

	while (!s.condvar)
	{
		// spin
	}

	rc = s.errcode;
#else
	int64_t res;

	int rc = ark_del((ARK*) ark, key_length, (void*) key_chars, &res);
#endif
	(*env)->ReleaseStringUTFChars(env, key, key_chars);

	if (rc == 0)
	{
		return JNI_TRUE;
	}

	if (rc == ENOENT)
	{
		// there is no such key
		return JNI_FALSE;
	}

	throwIOExceptionE(env, "Could not enqueue delete command", rc);
	return JNI_FALSE;
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    containsKey
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_contains_1key(
		JNIEnv *env, jclass cls, jlong ark, jstring key)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return JNI_FALSE;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

#if (USE_ASYNC)
	cb_info s;
	memset(&s, 0, sizeof(cb_info));

	int rc = ark_exists_async_cb(ensure_connected(ark), key_length, (void*) key_chars, &callback,
			(uint64_t) &s);

	if (rc != 0)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOExceptionE(env, "Could not enqueue exists command", rc);

		return JNI_FALSE;
	}

	while (!s.condvar)
	{
		// spin
	}

	rc = s.errcode;
#else
	int64_t res;
	int rc = ark_exists((ARK*) ark, key_length, (void*) key_chars, &res);
#endif
	(*env)->ReleaseStringUTFChars(env, key, key_chars);

	if (rc == 0)
	{
		return JNI_TRUE;
	}

	if (rc == ENOENT)
	{
		// there is no such key
		return JNI_FALSE;
	}

	// error case
	throwIOExceptionE(env, "Could not enqueue exists command", rc);
	return JNI_FALSE;
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    put_async
 * Signature: (JLjava/lang/String;Ljava/nio/ByteBuffer;ILcom/ibm/research/blockmap/Callback;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_put_1async(
		JNIEnv *env, jclass cls, jlong ark, jstring key, jobject value,
		jint size, jobject cb)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	void* buf = (*env)->GetDirectBufferAddress(env, value);
	if (buf == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIllegalStateException(env, "direct buffer address was NULL");
	}

	// prepare tag and create global references
	// to prevent callback and key from getting collected
	cb_tag *tag = create_tag(env, cb, key, key_chars, VOID_RESULT);

	int rc = ark_set_async_cb(ensure_connected(ark), key_length,
			(void*) key_chars, size, buf, &call_java_callback, (uint64_t) tag);

	if (rc == 0)
	{
		return;
	}

	// error case
	destroy_tag(env, tag);
	throwIOExceptionE(env, "Could not enqueue set command", rc);
}


/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    get_async
 * Signature: (JLjava/lang/String;Ljava/nio/ByteBuffer;Lcom/ibm/research/blockmap/Callback;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_get_1async(
		JNIEnv *env, jclass cls, jlong ark, jstring key, jobject buffer, jobject cb)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	void* buf = (*env)->GetDirectBufferAddress(env, buffer);
	if (buf == NULL)
	{
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOException(env, "Could not access buffer");
		return;
	}

	const jlong buf_size = (*env)->GetDirectBufferCapacity(env, buffer);
	if (buf_size < 0) {
		(*env)->ReleaseStringUTFChars(env, key, key_chars);
		throwIOException(env, "Invalid buffer");
		return;
	}

	// prepare tag and create global references
	// to prevent callback and key from getting collected
	cb_tag *tag = create_tag(env, cb, key, key_chars, buffer);

	int rc = ark_get_async_cb(ensure_connected(ark), key_length,
			(void*) key_chars, buf_size, buf, 0, &call_java_callback,
			(uint64_t) tag);

	if (rc == 0)
	{
		return;
	}

	// error case
	destroy_tag(env, tag);
	throwIOExceptionE(env, "Could not enqueue get command", rc);
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    delete_async
 * Signature: (JLjava/lang/String;Lcom/ibm/research/blockmap/Callback;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_delete_1async(
		JNIEnv *env, jclass cls, jlong ark, jstring key, jobject cb)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	// prepare tag and create global references
	// to prevent callback and key from getting collected
	cb_tag *tag = create_tag(env, cb, key, key_chars, BOOL_RESULT);

	int rc = ark_del_async_cb(ensure_connected(ark), key_length,
			(void*) key_chars, &call_java_callback, (uint64_t) tag);

	if (rc == 0)
	{
		return;
	}

	// error case
	destroy_tag(env, tag);
	throwIOExceptionE(env, "Could not enqueue delete command", rc);
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    containsKey_async
 * Signature: (JLjava/lang/String;Lcom/ibm/research/blockmap/Callback;)V
 */
JNIEXPORT void JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_containsKey_1async(
		JNIEnv *env, jclass cls, jlong ark, jstring key, jobject cb)
{
	UNUSED(cls);

	const char *key_chars = (*env)->GetStringUTFChars(env, key, NULL);
	if (key_chars == NULL)
	{
		throwOOMError(env, "Could not retrieve key_chars.");
		return;
	}
	uint64_t key_length = (*env)->GetStringUTFLength(env, key);

	// prepare tag and create global references
	// to prevent callback and key from getting collected
	cb_tag *tag = create_tag(env, cb, key, key_chars, BOOL_RESULT);

	int rc = ark_exists_async_cb(ensure_connected(ark), key_length,
			(void*) key_chars, &call_java_callback, (uint64_t) tag);

	if (rc == 0)
	{
		return;
	}

	// error case
	destroy_tag(env, tag);
	throwIOExceptionE(env, "Could not enqueue containsKey command", rc);
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice
 * Method:    get_iterator
 * Signature: (J)Ljava/util/Iterator;
 */
JNIEXPORT jobject JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_get_1iterator(
		JNIEnv *env, jclass cls, jlong ark)
{
	UNUSED(env);
	UNUSED(cls);
	ARI *ari;

	int64_t klen;

	char key[KEY_LENGTH];

	ari = ark_first((ARK *) ark, KEY_LENGTH, &klen, &key);

	key[klen] = 0;

	cache_class(env, key_iterator_cls, "com/ibm/research/blockmap/CapiKvDevice$KeyIterator", {
			key_iterator_constr = (*env)->GetMethodID(env, key_iterator_cls,
							"<init>", "(JLjava/lang/String;)V");
	}, return NULL);

	jstring first_key = (*env)->NewStringUTF(env, key);

	return (*env)->NewObject(env, key_iterator_cls, key_iterator_constr,
			(jlong) ari, first_key);
}

/*
 * Class:     com_ibm_research_blockmap_CapiKvDevice$KeyIterator
 * Method:    next_key
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_ibm_research_blockmap_CapiKvDevice_00024KeyIterator_next_1key(
		JNIEnv *env, jclass cls, jlong ari)
{
	UNUSED(cls);

	int64_t klen;

	char key[KEY_LENGTH];

	int rc = ark_next((ARI *) ari, KEY_LENGTH, &klen, &key);

	if (rc == 0)
	{
		key[klen] = 0;

		return (*env)->NewStringUTF(env, key);
	}

	if (rc == ENOENT)
	{
		return NULL;
	}

	throwIOExceptionE(env, "Error during key iteration", rc);
	return NULL;
}
