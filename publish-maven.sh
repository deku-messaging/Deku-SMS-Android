#!/bin/sh

# https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-the-pom

# Generate the secring file
# gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg

./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
