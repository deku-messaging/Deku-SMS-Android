FROM ubuntu:22.04 AS base

RUN apt update && apt install -y openjdk-17-jdk openjdk-17-jre android-sdk sdkmanager

WORKDIR /android

COPY . .

ENV ANDROID_HOME "/usr/lib/android-sdk/"
ENV PATH "${PATH}:${ANDROID_HOME}tools/:${ANDROID_HOME}platform-tools/"
# ENV GRADLE_OPTS "-Xmx2048m"

RUN yes | sdkmanager --licenses

ENV PASS=""
ENV MIN_SDK=""

# CMD ./gradlew assembleDebug
FROM base as apk-builder
CMD ./gradlew assembleRelease && \
apksigner sign --ks app/keys/app-release-key.jks \
--ks-pass pass:$PASS \
--in app/build/outputs/apk/release/app-release-unsigned.apk \
--out app/build/outputs/apk/release/app-release.apk

FROM base as bundle-builder
CMD ./gradlew assemble bundleRelease && \
apksigner sign --ks app/keys/app-release-key.jks \
--ks-pass pass:$PASS \
--in app/build/outputs/bundle/release/app-release.aab \
--out app/build/outputs/bundle/release/app-bundle.aab \
--min-sdk-version $MIN_SDK

# CMD cp app/build/outputs/apk/debug/app-debug.apk /apkbuilds/
# CMD sha256sum app/build/outputs/apk/debug/app-debug.apk
