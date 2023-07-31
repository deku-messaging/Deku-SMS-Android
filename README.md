# Deku SMS
<b>Contents</b>

[About](#about)

[Build](#build)

[Reproducible builds](#reproducible_builds)

# <a name="about"></a> About
Deku SMS is an Android SMS app.

<b>Features</b>
- End-to-End encryption
- Ability to forward incoming messages to personal cloud
- Ability to use mobile phone as an SMS Gateway to send messages from the cloud

# <a name="build"></a> Build
Getting the project into Android-studio would allow for an easy build.

## <a name="reproducible_builds"></a> Reproducible builds notes
- Create a file called `ks.passwd` at the root of the project.\
This file contains the keystore password for signing the .jks (keystore) file used for signing the apks.
- Copy your keystore file to `apps/keys/app-release-key.jks`.

<b>Pending to do</b>
- For reproducible builds, run `make release-docker`.\
This handles building 2 instances of the project in Docker containers for isolation. The output apks are signed and compared using `diffoscope`.


- https://f-droid.org/docs/Reproducible_Builds/
