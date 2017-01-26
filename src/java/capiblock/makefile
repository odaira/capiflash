# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: makefile $
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

JAVA_TEST_SRC:=test
JAVA_TEST_BIN:=$(BUILD_DIR)/test

JAVADOC_OUT:=doc

JDK_PATH?=$(JAVA_HOME)
JDK_INCLUDE_PATH:=$(JDK_PATH)/include
JAVA:=$(JDK_PATH)/bin/java
JAVAC:=$(JDK_PATH)/bin/javac
JAVAH:=$(JDK_PATH)/bin/javah
JAR:=$(JDK_PATH)/bin/jar
JAVADOC:=$(JDK_PATH)/bin/javadoc

PKG:=com.ibm.research.capiblock
PKG_PATH:=$(subst .,/,$(PKG))

JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/CapiBlockDevice.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/Chunk.class
JAVA_CLASSES+=$(JAVA_BIN)/$(PKG_PATH)/ObjectStreamBuffer.class

JAVA_TEST_FILES+=$(wildcard $(JAVA_TEST_SRC)/$(PKG_PATH)/*.java)
JAVA_TEST_CLASSES:=$(JAVA_TEST_FILES:$(JAVA_TEST_SRC)/%.java=$(JAVA_TEST_BIN)/%.class)
JAVA_TESTS:=$(subst /,.,$(JAVA_TEST_FILES:$(JAVA_TEST_SRC)/%.java=%))

LIB_NAME:=libcapiblock.so
LIB_PATH:=$(JNI_BIN)/
ARKDB_LIBNAME:=libcflsh_block.a

JNI_TYPE:=$(PKG).CapiBlockDevice
JNI_CLASS:=$(JAVA_BIN)/$(subst .,/,$(JNI_TYPE)).class
JNI_HEADER:=$(JNI_SRC)/include/capiblock_jni.h

INCLUDES+=-I$(JDK_INCLUDE_PATH)
INCLUDES+=-I$(JDK_INCLUDE_PATH)/linux
#for OpenJDK:
INCLUDES+=-I$(JNI_SRC)/include

CAPI_INCLUDES+=-I$(ROOTPATH)/src/include

C_FLAGS+=-fPIC

LD_FLAGS+=-Wl,-rpath=$(IMGDIR)
LD_FLAGS+=-L$(IMGDIR)
LD_FLAGS+=-lpthread
LD_FLAGS+=-lcflsh_block

.PHONY: all
.PHONY: clean
.PHONY: depclean
.PHONY: dumpclean

all: .dirs $(JAVA_CLASSES) $(EXTRA) $(LIB_PATH)$(LIB_NAME)
	echo "BUILDING STUFF"
	echo "TEST FILES ARE $(JAVA_TEST_FILES)"
	echo "JAVA TEST CLASSES ARE $(JAVA_TEST_CLASSES)"
	echo "LD_FLAGS ARE $(LD_FLAGS)"

include $(ROOTPATH)/config.mk

OBJECTS:=$(JNI_BIN)/capiblock.o

.dirs:
	mkdir -p $(JAVA_BIN)
	mkdir -p $(JNI_BIN)
	mkdir -p $(JAVA_TEST_BIN)

$(JAVA_CLASSES): $(JAVA_BIN)/%.class: $(JAVA_SRC)/%.java
	$(JAVAC) -sourcepath $(JAVA_SRC) -d $(JAVA_BIN) $<

$(OBJECTS): $(JNI_BIN)/%.o: $(JNI_SRC)/%.c
	$(CC) $(INCLUDES) $(CAPI_INCLUDES) $(C_FLAGS) -c $< -o $@

$(LIB_PATH)$(LIB_NAME): $(JNI_HEADER) $(OBJECTS)
	$(LD) -Wl,-soname=$(LIB_NAME) $(OBJECTS) $(LD_FLAGS) -shared -o $(LIB_PATH)$(LIB_NAME)

$(JAVA_TEST_CLASSES): $(JAVA_TEST_BIN)/%.class: $(JAVA_TEST_SRC)/%.java
	$(JAVAC) -cp $(JAVA_BIN):junit-4.3.jar -sourcepath $(JAVA_TEST_SRC) -d $(JAVA_TEST_BIN) $<

junit-4.3.jar:
	wget http://search.maven.org/remotecontent?filepath=junit/junit/4.3/junit-4.3.jar -O junit-4.3.jar

test: all junit-4.3.jar $(JAVA_TEST_CLASSES)
	$(JAVA) -XX:+ShowMessageBoxOnError -Xdump:none -Djava.library.path=$(JNI_BIN):$(IMGDIR) -cp $(JAVA_BIN):$(JAVA_TEST_BIN):junit-4.3.jar org.junit.runner.JUnitCore $(JAVA_TESTS)       

$(NATIVE_BIN)/native_test.o: $(NATIVE_SRC)/native_test.c
	$(CC) $(INCLUDES) $(C_FLAGS) -c $< -o $@

capiblock.jar: .dirs $(JAVA_CLASSES) $(EXTRA)
	$(JAR) -cvf capiblock.jar -C $(JAVA_BIN) . 

jartest: capiblock.jar $(LIB_PATH)$(LIB_NAME)
	$(JAVAC) -sourcepath $(JAVA_TEST_SRC) -cp $(JAVA_BIN) -d $(JAVA_TEST_BIN) $(JAVA_TEST_SRC)/$(PKG_PATH)/jar/JarTest.java	
	$(JAVA) -Djava.library.path=$(JNI_BIN) -cp capiblock.jar:$(JAVA_TEST_BIN) $(PKG).jar.JarTest
	rm -rf $(JAVA_TEST_BIN)/$(PKG_PATH)/jar

doc: $(JAVA_CLASSES)
	$(JAVADOC) -classpath $(JAVA_BIN) -sourcepath $(JAVA_SRC) -d $(JAVADOC_OUT) -public -author -version $(PKG)

example: capiblock.jar
	$(JAVAC) -sourcepath $(JAVA_TEST_SRC) -cp $(JAVA_BIN) -d $(JAVA_TEST_BIN) $(JAVA_TEST_SRC)/$(PKG_PATH)/example/Example.java
	$(JAVA) -Djava.library.path=$(JNI_BIN) -cp capiblock.jar:$(JAVA_TEST_BIN) $(PKG).example.Example
        
dist: capiblock.jar $(LIB_PATH)$(LIB_NAME) example javadoc
	mkdir capiblock
	cp capiblock.jar capiblock/
	cp $(LIB_PATH)$(LIB_NAME) capiblock/
	cp -r $(JAVADOC_OUT) capiblock/
	mkdir -p capiblock/example/$(PKG_PATH)/example
	cp -r $(JAVA_TEST_SRC)/$(PKG_PATH)/example/Example.java capiblock/example/$(PKG_PATH)/example/
	$(JAVAC) -sourcepath capiblock/example -cp capiblock/capiblock.jar -d capiblock/example capiblock/example/$(PKG_PATH)/example/Example.java
	echo "\$$JAVA_HOME/bin/java -Djava.library.path=. -cp example:capiblock.jar com.ibm.research.capiblock.example.Example" > capiblock/run_example.sh      
	tar cvzf capiblock.tar.gz capiblock/

clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(LIB_PATH)$(LIB_NAME)
	rm -rf $(JAVADOC_OUT)
	rm -rf capiblock.jar
	rm -rf capiblock

distclean: clean
	rm -f junit-4.3.jar

dumpclean:
	rm -f jitdump*
	rm -f javacore*
	rm -f Snap*
	rm -f core*
	rm -f hs_err*
