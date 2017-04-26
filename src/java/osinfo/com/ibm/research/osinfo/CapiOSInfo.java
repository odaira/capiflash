/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/capiblock/com/ibm/research/capiblock/CapiBlockDevice.java $
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
package com.ibm.research.osinfo;

public class CapiOSInfo {
    public static void main(String args[]) {
	if (args.length != 1) {
	    System.exit(1);
	}
	if (args[0].equals("--name")) {
	    String osName = System.getProperty("os.name");
	    if (osName.contains("Linux")) {
		System.out.println("linux");
	    } else {
		System.out.println("unknown");
	    }
	} else if (args[0].equals("--arch")) {
	    System.out.println(System.getProperty("os.arch"));
	}
    }
}

