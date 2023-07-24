FROM ubuntu:22.04

RUN apt update && apt install -y openjdk-17-jdk openjdk-17-jre android-sdk sdkmanager

WORKDIR /android

COPY . .

ENV ANDROID_HOME "/usr/lib/android-sdk/"
ENV PATH "${PATH}:${ANDROID_HOME}tools/:${ANDROID_HOME}platform-tools/"
# ENV GRADLE_OPTS "-Xmx2048m"

RUN yes | sdkmanager --licenses

CMD ./gradlew assembleDebug

# CMD cp app/build/outputs/apk/debug/app-debug.apk /apkbuilds/
# CMD sha256sum app/build/outputs/apk/debug/app-debug.apk
