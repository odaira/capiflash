/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/jni/include/blockmap_ex.h $
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

#ifndef BLOCKMAP_EX_H_
#define BLOCKMAP_EX_H_

// maximum error message langth
#define MSG_LEN 256

static inline void throwIOException(JNIEnv* env, const char* msg)
{
	jclass exceptionClass = (*env)->FindClass(env, "java/io/IOException");
	if (NULL == exceptionClass)
	{
		// if this class cannot be found there is already an exception set
		return;
	}
	(*env)->ThrowNew(env, exceptionClass, msg);
}

static inline void throwIOExceptionE(JNIEnv* env, const char* msg, int err)
{
	char message[MSG_LEN];
	snprintf(message, MSG_LEN, "%s (Error Code=%d, %s)", msg, err,
			strerror(err));
	throwIOException(env, message);
}

static inline void throwOOMError(JNIEnv* env, const char* msg)
{
	jclass exceptionClass = (*env)->FindClass(env,
			"java/lang/OutOfMemoryError");
	if (NULL == exceptionClass)
	{
		// if this class cannot be found there is already an exception set
		return;
	}
	(*env)->ThrowNew(env, exceptionClass, msg);
}

static inline void throwIllegalStateException(JNIEnv* env, const char* msg)
{
	jclass exceptionClass = (*env)->FindClass(env,
			"java/lang/IllegalStateException");
	if (NULL == exceptionClass)
	{
		// if this class cannot be found there is already an exception set
		return;
	}
	(*env)->ThrowNew(env, exceptionClass, msg);
}


#endif /* BLOCKMAP_EX_H_ */
