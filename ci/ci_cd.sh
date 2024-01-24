#!/usr/bin/bash

git clone --recurse-submodules -j8 git@github.com:deku-messaging/Deku-SMS-Android.git
cd Deku-SMS-Android && \
	git checkout staging && \
	git submodule update --init --recursive && \
	cp ../../release.properties . && \
	make release-complete jks_pass="$1" && cd .. \
rm -r Deku-SMS-Android
