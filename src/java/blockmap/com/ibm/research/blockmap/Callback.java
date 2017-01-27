/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/com/ibm/research/blockmap/Callback.java $
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

/**
 * The Interface Callback.
 *
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 * 
 * @author Jan S. Rellermeyer, IBM Research
 * @version 1.0.0
 */
public interface Callback<K, V> {

	/**
	 * This method is called when the operation has completed successfully.
	 *
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
	void operationCompleted(K key, V value);

	/**
	 * This method is called when the operation failed.
	 *
	 * @param key
	 *            the key
	 * @param e
	 *            the exception indicating the nature of the failure.
	 */
	void operationFailed(K key, Exception e);

	/**
	 * Callback for the asynchronous
	 * {@link com.ibm.research.blockmap.AbstractBlockMap#getAsync(java.lang.String,com.ibm.research.blockmap.Callback)}
	 * method.
	 *
	 * @param <B>
	 *            the generic type
	 */
	public interface Get<B extends AbstractBlockMap.Block> extends
			Callback<String, B> {
	}

	/**
	 * Callback for the asynchronous
	 * {@link com.ibm.research.blockmap.AbstractBlockMap#putAsync(java.lang.String,com.ibm.research.blockmap.AbstractBlockMap.Block,com.ibm.research.blockmap.Callback)}
	 * method.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public interface Put extends Callback<String, Void> {
	}

	/**
	 * Callback for the asynchronous
	 * {@link com.ibm.research.blockmap.AbstractBlockMap#deleteAsync(java.lang.String,com.ibm.research.blockmap.Callback)}
	 * method.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public interface Delete extends Callback<String, Boolean> {
	}

	/**
	 * Callback for the asynchronous
	 * {@link com.ibm.research.blockmap.AbstractBlockMap#containsKeyAsync(java.lang.String,com.ibm.research.blockmap.Callback)}
	 * method.
	 * 
	 * @author Jan S. Rellermeyer, IBM Research
	 * @version 1.0.0
	 */
	public interface ContainsKey extends Callback<String, Boolean> {
	}
}
