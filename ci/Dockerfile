FROM ubuntu:jammy-20230624@sha256:b060fffe8e1561c9c3e6dea6db487b900100fc26830b9ea2ec966c151ab4c020

# Install dependencies in a single layer
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    openjdk-17-jdk \
    unzip \
    wget \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /android

# Set Android SDK Environment Variables
ENV ANDROID_HOME=/usr/local/android-sdk-linux
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk-linux
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
ENV ANDROID_BUILD_TOOLS_VERSION=37.0.0

# Download and place commandline-tools into cmdline-tools/latest
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    DOWNLOAD_URL=$(curl -s https://developer.android.com/studio \
      | grep -o 'https://dl.google.com/android/repository/commandlinetools-linux-[0-9]*_latest.zip' \
      | head -1) && \
    wget -q "$DOWNLOAD_URL" -O /tmp/commandlinetools.zip && \
    unzip -q /tmp/commandlinetools.zip -d /tmp/cmdline-tools && \
    mv /tmp/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -rf /tmp/commandlinetools.zip /tmp/cmdline-tools

RUN chmod -R 777 ${ANDROID_HOME}

# Accept licenses first
RUN yes | sdkmanager --licenses --sdk_root="${ANDROID_HOME}"
# RUN yes | sdkmanager --sdk_root="${ANDROID_HOME}" --include_preview --channel=3 \
#    "platforms;android-37" || true

# Install valid stable packages (excluding non-existent android-37)
RUN yes | sdkmanager --sdk_root="${ANDROID_HOME}" \
    "platform-tools" \
    "platforms;android-37.0" \
    "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "extras;google;m2repository" \
    "extras;android;m2repository" \
    "extras;google;google_play_services" \
    "ndk;28.0.13004108" \
    "cmake;3.22.1"

# Copy source after installing SDK dependencies to maximize Docker layer caching
COPY . .

RUN git config --global --add safe.directory /project