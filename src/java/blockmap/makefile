# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: src/java/blockmap/config.mk $
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

KRYO?=1

JAVA_SRC:=.
JAVA_BIN:=$(BUILD_DIR)/java

JNI_SRC:=jni
JNI_BIN:=$(BUILD_DIR)/jni

JAVA_TEST_SRC:=test
JAVA_TEST_BIN:=$(BUILD_DIR)/test

KRYO_SRC:=kryo
KRYO_DEP_DIR:=kryo_libs

JAVADOC_OUT:=doc

PKG:=com.ibm.research.blockmap
PKG_PATH:=$(subst .,/,$(PKG))

JAVA_CLASSES:=$(JAVA_BIN)/$(PKG_PATH)/CapiKvDevice.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/Callback.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/AbstractBlockMap.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/BlockMap.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/ObjectStreamBlockMap.class

LIB_NAME:=libblockmap.so
LIB_PATH:=$(JNI_BIN)/

JNI_TYPE:=$(PKG).CapiBlockDevice
JNI_CLASS:=$(JAVA_BIN)/$(subst .,/,$(JNI_TYPE)).class
JNI_HEADER:=$(JNI_SRC)/include/blockmap_jni.h

JAR_FILENAME:=blockmap.jar
DIST_FILENAME:=blockmap.tar.gz
DIST_EXTRA_TARGETS:=

OBJECTS:=$(JNI_BIN)/blockmap.o

LD_FLAGS+=-lpthread
LD_FLAGS+=-larkdb

OBJENESIS_VER:=1.2
OBJENESIS_JAR:=objenesis-$(OBJENESIS_VER).jar
OBJENESIS_URL:=http://central.maven.org/maven2/org/objenesis/objenesis/$(OBJENESIS_VER)/$(OBJENESIS_JAR)
REFLECTASM_VER:=1.09
REFLECTASM_JAR:=reflectasm-$(REFLECTASM_VER)-shaded.jar
REFLECTASM_URL:=http://central.maven.org/maven2/com/esotericsoftware/reflectasm/reflectasm/$(REFLECTASM_VER)/$(REFLECTASM_JAR)
MINLOG_VER:=1.2
MINLOG_JAR:=minlog-$(MINLOG_VER).jar
MINLOG_URL:=http://central.maven.org/maven2/com/esotericsoftware/minlog/minlog/$(MINLOG_VER)/$(MINLOG_JAR)
KRYO_VER:=3.0.1
KRYO_JAR:=kryo-$(KRYO_VER).jar
KRYO_URL:=http://central.maven.org/maven2/com/esotericsoftware/kryo/$(KRYO_VER)/$(KRYO_JAR)

KRYO_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/kryo/KryoBlockMap.class

KRYO_DEPS:=$(JAVA_BIN):$(KRYO_DEP_DIR)/$(KRYO_JAR):$(KRYO_DEP_DIR)/$(MINLOG_JAR):$(KRYO_DEP_DIR)/$(OBJENESIS_JAR):$(KRYO_DEP_DIR)/$(REFLECTASM_JAR)

ifeq ($(KRYO), 1)
EXTRA+=$(KRYO_CLASSES)
endif

$(KRYO_DEP_DIR):
	mkdir -p $(KRYO_DEP_DIR)
	wget $(OBJENESIS_URL) -O $(KRYO_DEP_DIR)/$(OBJENESIS_JAR)
	wget $(REFLECTASM_URL) -O $(KRYO_DEP_DIR)/$(REFLECTASM_JAR)
	wget $(MINLOG_URL) -O $(KRYO_DEP_DIR)/$(MINLOG_JAR)
	wget $(KRYO_URL) -O $(KRYO_DEP_DIR)/$(KRYO_JAR)

$(KRYO_CLASSES): $(JAVA_BIN)/%.class: $(KRYO_SRC)/%.java $(KRYO_DEP_DIR)
	$(JAVAC) -cp $(KRYO_DEPS) -sourcepath $(KRYO_SRC) -d $(JAVA_BIN) $<

include ../config.mk

NATIVE_SRC:=native_test
NATIVE_BIN:=$(BUILD_DIR)/native_test

$(NATIVE_BIN)/native_test.o: $(NATIVE_SRC)/native_test.c
	$(CC) $(INCLUDES) -I$(ROOTPATH)/src/kv $(C_FLAGS) -std=c99 -c $< -o $@

$(NATIVE_BIN):
	mkdir -p $(NATIVE_BIN)	
	
native_test: .dirs $(NATIVE_BIN) $(NATIVE_BIN)/native_test.o	
	$(CC) $(NATIVE_BIN)/native_test.o $(LD_FLAGS) -o $(NATIVE_BIN)/native_test
	$(NATIVE_BIN)/native_test
