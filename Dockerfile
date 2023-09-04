FROM ubuntu:22.04

RUN apt update && apt install -y openjdk-17-jdk openjdk-17-jre android-sdk sdkmanager

WORKDIR /android

COPY . .

ENV ANDROID_HOME "/usr/lib/android-sdk/"
ENV PATH "${PATH}:${ANDROID_HOME}tools/:${ANDROID_HOME}platform-tools/"
# ENV GRADLE_OPTS "-Xmx2048m"

RUN yes | sdkmanager --licenses

ENV PASS=""

# CMD ./gradlew assembleDebug
CMD ./gradlew \
-Dorg.gradle.daemon=true \
-Dorg.gradle.buildCache=false \
assembleRelease && \
apksigner sign --ks app/keys/app-release-key.jks \
--ks-pass pass:$PASS \
--in app/build/outputs/apk/release/app-release-unsigned.apk \
--out app/build/outputs/apk/release/app-release.apk && \
./gradlew assemble bundleRelease && \
apksigner sign --ks app/keys/app-release-key.jks \
--ks-pass pass:$PASS \
--in app/build/outputs/bundle/release/app-release.aab \
--out app/build/outputs/bundle/release/app-bundle.apk

# CMD cp app/build/outputs/apk/debug/app-debug.apk /apkbuilds/
# CMD sha256sum app/build/outputs/apk/debug/app-debug.apk
