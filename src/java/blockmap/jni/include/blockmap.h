/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/jni/include/blockmap.h $
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

#ifndef BLOCKMAP_H_
#define BLOCKMAP_H_

#ifdef UNUSED
#undef UNUSED
#endif

#define UNUSED(x) (void)(x);

#if TRACE_ENABLE
#define TRACE(...)	fprintf(stderr, "[TRACE] %s(%d): ",__FILE__, __LINE__ ); fprintf(stderr, __VA_ARGS__);
#else
#define TRACE(...)
#endif

#include <stdint.h>

#define cache_class(env, cls, name, init, exit) \
  if (cls == NULL)								\
  {												\
	jclass _cls = (*env)->FindClass(env, name); \
	if (_cls == NULL)							\
	{											\
	  throwIllegalStateException(env, "Could not find class"); \
	  exit;										\
	} 											\
	cls = (*env)->NewGlobalRef(env, _cls);		\
	init										\
  }

#define uncache_class(env, cls)					\
  if (cls != NULL)								\
  {												\
    (*env)->DeleteGlobalRef(env, cls);			\
  }

typedef struct
{
	jobject callback_object;
	jstring key;
	const char* key_chars;
	jobject result;
} cb_tag;

static inline cb_tag *create_tag(JNIEnv *env, jobject cb, jstring key, const char* key_chars, jobject result)
{
	cb_tag *i = malloc(sizeof(cb_tag));
	i->callback_object = cb == NULL ? NULL : (*env)->NewGlobalRef(env, cb);
	i->key = (*env)->NewGlobalRef(env, key);
	i->key_chars = key_chars;
	if (((uint64_t) result) > 0) {
		i->result = (*env)->NewGlobalRef(env, result);;
	} else {
		i->result = result;
	}

	TRACE("tag created with key_chars %p\n", (void*) key_chars);

	return i;
}

static inline void destroy_tag(JNIEnv *env, cb_tag *tag)
{
	(*env)->ReleaseStringUTFChars(env, tag->key, tag->key_chars);
	(*env)->DeleteGlobalRef(env, tag->key);
	if (tag->callback_object != NULL)
	{
		(*env)->DeleteGlobalRef(env, tag->callback_object);
	}
	free(tag);
}

static inline void CHECK_EXCEPTION(JNIEnv *env)
{
	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionDescribe(env);
	}
}

#endif /* BLOCKMAP_H_ */
