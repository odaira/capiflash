/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/jni/include/capiblock_lib.h $
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

#ifndef CAPIBLOCK_LIB_H_
#define CAPIBLOCK_LIB_H_

#ifdef UNUSED
#undef UNUSED
#endif

#define UNUSED(x) (void)(x);

#if TRACE_ENABLE
#define TRACE(...)	fprintf(stderr, "[TRACE] %s(%d): ",__FILE__, __LINE__ ); fprintf(stderr, __VA_ARGS__);
#else
#define TRACE(...)
#endif

#define cache_class(env, cls, name, init, exit) \
  if (cls == NULL)								      \
  {												         \
	jclass _cls = (*env)->FindClass(env, name);  \
	if (_cls == NULL)							         \
	{											            \
	  throwIllegalStateException(env, "Could not find class");  \
	  exit;										                        \
	} 											                           \
	cls = (*env)->NewGlobalRef(env, _cls);		                  \
	init										                           \
  }

#define uncache_class(env, cls)					\
  if (cls != NULL)								   \
  {												      \
    (*env)->DeleteGlobalRef(env, cls);			\
  }

#endif // CAPIBLOCK_LIB_H_
