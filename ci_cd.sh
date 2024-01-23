#!/usr/bin/bash

git clone --recurse-submodules -j8 git@github.com:deku-messaging/Deku-SMS-Android.git && \
	cd Deku-SMS-Android && \
	make release-cd
