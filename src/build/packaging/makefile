# IBM_PROLOG_BEGIN_TAG
# This is an automatically generated prolog.
#
# $Source: src/build/packaging/makefile $
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
UNAME=$(shell uname)

ROOTPATH = ../../..
ifeq ($(UNAME),AIX)
all: aixcxlflash aixcxlflash-test aixcxlflashimage
else

#only add tarpkgs if we are in the DISABLED dir path
DISABLED=$(shell pwd|grep DISABLED)
ifeq "$(DISABLED)" ""
all: cxlflash cxlflash-test cxlflashimage
else
all: cxlflash cxlflash-test cxlflashimage tarpkgs
endif

endif

VERSIONMAJOR=4
VERSIONMINOR=1
SHORT_VERSION=${VERSIONMAJOR}.${VERSIONMINOR}
VERSION=${VERSIONMAJOR}.${VERSIONMINOR}-${GITREVISION}

cxlflash:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	fpm -f -s dir -t rpm -n $@ -v ${VERSION} --iteration=1 \
		--depends 'advance-toolchain-at10.0-runtime' \
		--depends 'systemd-devel' \
		-C ./install_root \
		--after-install ${SURELOCKROOT}/src/build/install/resources/postinstall \
		--after-remove ${SURELOCKROOT}/src/build/install/resources/postremove \
		--conflicts ibmcapikv --replaces ibmcapikv .; \
	fpm -f -s dir -t deb -n $@ -v ${VERSION} \
		--depends 'advance-toolchain-at10.0-runtime' \
		--depends 'libudev1' \
		-C ./install_root \
		--after-install ${SURELOCKROOT}/src/build/install/resources/postinstall \
		--after-remove ${SURELOCKROOT}/src/build/install/resources/postremove \
		--conflicts ibmcapikv --replaces ibmcapikv .; \
	tar -cvzf $@-${GITREVISION}.tar.gz -C ./install_root .

cxlflash-test:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	fpm -f -s dir -t rpm -n $@ -v ${VERSION} -C ./test_root --iteration=1 \
		--depends 'cxlflash = ${VERSION}-1' \
		--depends 'systemd-devel' \
		--conflicts ibmcapikv-test --replaces ibmcapikv-test .; \
	fpm -f -s dir -t deb -n $@ -v ${VERSION} -C ./test_root \
		--depends 'cxlflash = ${VERSION}' \
		--depends 'libudev-dev' \
		--conflicts ibmcapikv-test --replaces ibmcapikv-test .; \
	tar -cvzf $@-${GITREVISION}.tar.gz -C ./test_root .

cxlflashimage:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	fpm -f -s dir -t rpm -a all -n $@ -v ${VERSION} -C ./afu_root --iteration=1 \
		--after-install ${SURELOCKROOT}/src/build/install/resources/postafuinstall \
		--conflicts afuimage --replaces afuimage .; \
	fpm -f -s dir -t deb -a all -n $@ -v ${VERSION} -C ./afu_root \
		--after-install ${SURELOCKROOT}/src/build/install/resources/postafuinstall \
		--conflicts afuimage --replaces afuimage .; \

tarpkgs:
	cd ${PKGDIR}; \
	echo ${VERSION} > ver.txt; \
	tar -cvzf pkg.${VERSION}.rpm.tar.gz *${SHORT_VERSION}*.rpm cflash_configure ver.txt; \
	tar -cvzf pkg.${VERSION}.deb.tar.gz *${SHORT_VERSION}*.deb cflash_configure ver.txt; \
	cp pkg.${VERSION}*deb.tar.gz /gsa/ausgsa/projects/s/surelock/surelock-sw/releases/GA4/latest_jenkins/pkg.deb.tar.gz; \
	cp pkg.${VERSION}*rpm.tar.gz /gsa/ausgsa/projects/s/surelock/surelock-sw/releases/GA4/latest_jenkins/pkg.rpm.tar.gz

aixcxlflash:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	tar -cvf cxlflash_${VERSION}.ppc64be.tar -C ./install_root .; \
	gzip -f cxlflash_${VERSION}.ppc64be.tar

aixcxlflash-test:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	tar -cvf cxlflash-test_${VERSION}.ppc64be.tar -C ./test_root .; \
	gzip -f cxlflash-test_${VERSION}.ppc64be.tar

aixcxlflashimage:
	mkdir -p ${PKGDIR}
	cd ${PKGDIR}; \
	tar -cvf cxlflashimage_${VERSION}.ppc64be.tar -C ./afu_root .; \
	gzip -f cxlflashimage_${VERSION}.ppc64be.tar

include ${ROOTPATH}/config.mk
