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

JDK_PATH?=$(JAVA_HOME)
JDK_INCLUDE_PATH:=$(JDK_PATH)/include
JAVA:=$(JDK_PATH)/bin/java
JAVAC:=$(JDK_PATH)/bin/javac
JAVAH:=$(JDK_PATH)/bin/javah
JAR:=$(JDK_PATH)/bin/jar
JAVADOC:=$(JDK_PATH)/bin/javadoc

JAVA_TEST_FILES+=$(wildcard $(JAVA_TEST_SRC)/$(PKG_PATH)/*.java)
JAVA_TEST_CLASSES:=$(JAVA_TEST_FILES:$(JAVA_TEST_SRC)/%.java=$(JAVA_TEST_BIN)/%.class)
JAVA_TESTS:=$(subst /,.,$(JAVA_TEST_FILES:$(JAVA_TEST_SRC)/%.java=%))

OSINFO_CLASS:=com.ibm.research.osinfo.CapiOSInfo
OS_NAME:=$(shell $(JAVA) -cp ../osinfo $(OSINFO_CLASS) --name)
OS_ARCH:=$(shell $(JAVA) -cp ../osinfo $(OSINFO_CLASS) --arch)

JUNIT_VERSION:=4.3
JUNIT_JAR:=junit-$(JUNIT_VERSION).jar

INCLUDES+=-I$(JDK_INCLUDE_PATH)
INCLUDES+=-I$(JDK_INCLUDE_PATH)/linux
#for OpenJDK:
INCLUDES+=-I$(JNI_SRC)/include

INCLUDES+=-I$(ROOTPATH)/src/include

C_FLAGS+=-fPIC

LD_FLAGS+=-Wl,-rpath=$(IMGDIR)
LD_FLAGS+=-L$(IMGDIR)

.PHONY: all
.PHONY: clean
.PHONY: depclean
.PHONY: dumpclean
.DEFAULT_GOAL:= all

all: .dirs $(JAVA_CLASSES) $(EXTRA) $(LIB_PATH)$(LIB_NAME) doc dist

include $(ROOTPATH)/config.mk

TMP_DIR:=$(subst .jar,,$(JAR_FILENAME))

.dirs:
	mkdir -p $(JAVA_BIN)
	mkdir -p $(LIB_PATH)
	mkdir -p $(JNI_BIN)
	mkdir -p $(JAVA_TEST_BIN)

$(JAVA_CLASSES): $(JAVA_BIN)/%.class: $(JAVA_SRC)/%.java
	$(JAVAC) -sourcepath $(JAVA_SRC) -d $(JAVA_BIN) $<

$(OBJS): $(JNI_BIN)/%.o: $(JNI_SRC)/%.c
	$(CC) $(INCLUDES) $(C_FLAGS) -c $< -o $@

$(LIB_PATH)$(LIB_NAME): $(JNI_HEADER) $(OBJS)
	echo "OBJECTS ARE $(OBJS)"
	$(LD) -Wl,-soname=$(LIB_NAME) $(OBJS) $(LD_FLAGS) -shared -o $(LIB_PATH)$(LIB_NAME)

$(JAVA_TEST_CLASSES): $(JAVA_TEST_BIN)/%.class: $(JAVA_TEST_SRC)/%.java
	$(JAVAC) -cp $(JAVA_BIN):$(JUNIT_JAR) -sourcepath $(JAVA_TEST_SRC) -d $(JAVA_TEST_BIN) $<

$(JUNIT_JAR):
	wget http://search.maven.org/remotecontent?filepath=junit/junit/$(JUNIT_VERSION)/$(JUNIT_JAR) -O $(JUNIT_JAR)

test: all $(JUNIT_JAR) $(JAVA_TEST_CLASSES)
	$(JAVA) -XX:+ShowMessageBoxOnError -Xdump:none -Djava.library.path=$(JNI_BIN):$(IMGDIR) -cp $(JAVA_BIN):$(JAVA_TEST_BIN):$(JUNIT_JAR) org.junit.runner.JUnitCore $(JAVA_TESTS)       

$(JAR_FILENAME): .dirs $(JAVA_CLASSES) $(LIB_PATH)$(LIB_NAME) $(EXTRA)
	$(JAR) -cvf $(JAR_FILENAME) -C $(JAVA_BIN) . 

jartest: $(JAR_FILENAME) $(LIB_PATH)$(LIB_NAME)
	$(JAVAC) -sourcepath $(JAVA_TEST_SRC) -cp $(JAVA_BIN) -d $(JAVA_TEST_BIN) $(JAVA_TEST_SRC)/$(PKG_PATH)/jar/JarTest.java	
	$(JAVA) -Djava.library.path=$(JNI_BIN) -cp $(JAR_FILENAME):$(JAVA_TEST_BIN) $(PKG).jar.JarTest
	rm -rf $(JAVA_TEST_BIN)/$(PKG_PATH)/jar

doc: $(JAVA_CLASSES)
	$(JAVADOC) -classpath $(JAVA_BIN) -sourcepath $(JAVA_SRC) -d $(JAVADOC_OUT) -public -author -version $(PKG)

$(TMP_DIR):
	mkdir $(TMP_DIR)
	        
dist: $(JAR_FILENAME) $(LIB_PATH)$(LIB_NAME) $(TMP_DIR) $(DIST_EXTRA_TARGETS) doc
	cp $(JAR_FILENAME) $(TMP_DIR)/
	cp -r $(JAVADOC_OUT) $(TMP_DIR)/
	tar cvzf $(DIST_FILENAME) $(TMP_DIR)/
	rm -rf $(TMP_DIR)

clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(LIB_PATH)$(LIB_NAME)
	rm -rf $(JAVADOC_OUT)
	rm -rf $(JAR_FILENAME)
	rm -rf $(DIST_FILENAME)

distclean:: clean
	rm -f $(JUNIT_JAR)

dumpclean:
	rm -f jitdump*
	rm -f javacore*
	rm -f Snap*
	rm -f core*
	rm -f hs_err*
