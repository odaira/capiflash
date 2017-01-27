# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: src/java/capiblock/makefile $
#
# IBM Data Engine for NoSQL - Power Systems Edition User Library Project
#
# Contributors Listed Below - COPYRIGHT 2014,2015
# [+] International Business Machines Corp.
#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#
# IBM_PROLOG_END_TAG

# @author Jan S. Rellermeyer, IBM Research

ROOTPATH = ../../..

BUILD_DIR:=build

JAVA_SRC:=.
JAVA_BIN:=$(BUILD_DIR)/java

JNI_SRC:=jni
JNI_BIN:=$(BUILD_DIR)/jni

OBJS:=$(JNI_BIN)/capiblock.o

JAVA_TEST_SRC:=test
JAVA_TEST_BIN:=$(BUILD_DIR)/test

JAVADOC_OUT:=doc

PKG:=com.ibm.research.capiblock
PKG_PATH:=$(subst .,/,$(PKG))

JAVA_CLASSES:=$(JAVA_BIN)/$(PKG_PATH)/CapiBlockDevice.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/Chunk.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/ObjectStreamBuffer.class

LIB_NAME:=libcapiblock.so
LIB_PATH:=$(JNI_BIN)/

JNI_TYPE:=$(PKG).CapiBlockDevice
JNI_CLASS:=$(JAVA_BIN)/$(subst .,/,$(JNI_TYPE)).class
JNI_HEADER:=$(JNI_SRC)/include/capiblock_jni.h

JAR_FILENAME:=capiblock.jar
DIST_FILENAME:=capiblock.tar.gz
DIST_EXTRA_TARGETS:=example copy_example

LD_FLAGS+=-lpthread
LD_FLAGS+=-lcflsh_block

.PHONY: example
.PHONY: copy_example

example: $(JAR_FILENAME)
	$(JAVAC) -sourcepath $(JAVA_TEST_SRC) -cp $(JAVA_BIN) -d $(JAVA_TEST_BIN) $(JAVA_TEST_SRC)/$(PKG_PATH)/example/Example.java
	
copy_example:
	mkdir -p capiblock/example/$(PKG_PATH)/example
	cp -r $(JAVA_TEST_SRC)/$(PKG_PATH)/example/Example.java capiblock/example/$(PKG_PATH)/example/
	$(JAVAC) -sourcepath $(TMP_DIR)/example -cp capiblock.jar -d capiblock/example capiblock/example/$(PKG_PATH)/example/Example.java
	echo "\$$JAVA_HOME/bin/java -Djava.library.path=. -cp example:capiblock.jar com.ibm.research.capiblock.example.Example" > capiblock/run_example.sh

include ../config.mk