FROM ubuntu:22.04 AS base

RUN apt update && \
apt install -y \
openjdk-17-jdk \
openjdk-17-jre \
android-sdk \
sdkmanager \
wget \
make \
git

WORKDIR /android
COPY . .

ENV ANDROID_HOME "/usr/lib/android-sdk/"
ENV PATH "${PATH}:${ANDROID_HOME}tools/:${ANDROID_HOME}platform-tools/"

RUN yes | sdkmanager --licenses

ENV PASS=""
ARG COMMIT=""
ARG COMMIT_URL=""
ARG RELEASE_URL=""

RUN wget -O app.apk $RELEASE_URL
RUN git clone $COMMIT_URL app
RUN cd app && \
	git checkout $COMMIT && \
	make && \
	git submodule update --init --recursive
RUN cp *.jks app/app/keys/

CMD cd app && \
./gradlew assembleRelease && \
apksigner sign --ks app/keys/app-release-key.jks \
--ks-pass pass:$PASS \
--in app/build/outputs/apk/release/app-release-unsigned.apk \
--out app/build/outputs/apk/release/app-release.apk && \
shasum ../app.apk && shasum app/build/outputs/apk/release/app-release.apk
