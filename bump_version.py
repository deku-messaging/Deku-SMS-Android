#!/usr/bin/python3
import os
import sys
import requests

def get_latest_tag():
    """Get the latest tag from the repository."""
    url = "https://api.github.com/repos/deku-messaging/Deku-SMS-Android/tags"
    response = requests.get(url)
    response.raise_for_status()

    return response.json()[0]["name"]

def bump_version(filename, flavour):
    """
    Bumps the version number in the specified file.

    Args:
    filename: The name of the file to update.

    Returns:
    The new version number.
    """

    with open(filename, "r") as f:
        lines = f.readlines()

    releaseVersion = None
    stagingVersion = None
    nightlyVersion = None
    tagVersion = get_latest_tag()

    for line in lines:
        if line.startswith("releaseVersion="): 
            releaseVersion = line.split("=")[1].strip() 

        if line.startswith("stagingVersion="): 
            stagingVersion = line.split("=")[1].strip() 

        if line.startswith("nightlyVersion="): 
            nightlyVersion = line.split("=")[1].strip() 

        """
        if line.startswith("tagVersion="): 
            tagVersion = line.split("=")[1].strip() 
        """

    if releaseVersion is None:
        raise ValueError("Could not find releaseVersion in file")

    if stagingVersion is None:
        raise ValueError("Could not find stagingVersion in file")

    if nightlyVersion is None:
        raise ValueError("Could not find nightlyVersion in file")

    if tagVersion is None:
        raise ValueError("Could not find tagVersion in file")

    if flavour == "refs/heads/master":
        releaseVersion = int(releaseVersion) + 1
        stagingVersion = 0
        nightlyVersion = 0

    elif flavour == "refs/heads/staging":
        stagingVersion = int(stagingVersion) + 1
        nightlyVersion = 0

    else:
        nightlyVersion = int(nightlyVersion) + 1

    tagVersion = int(tagVersion) + 1

    with open(filename, "w") as f:
        f.write("releaseVersion=" + str(releaseVersion) + "\n")
        f.write("stagingVersion=" + str(stagingVersion) + "\n")
        f.write("nightlyVersion=" + str(nightlyVersion) + "\n")
        f.write("tagVersion=" + str(tagVersion))

    return releaseVersion, stagingVersion, nightlyVersion, tagVersion


if __name__ == "__main__":
  filename = "version.properties"

  flavour = sys.argv[1]
  releaseVersion, stagingVersion, nightlyVersion, tagVersion = bump_version(filename, flavour)
  print("+ successful version bump: ",
        releaseVersion, stagingVersion, nightlyVersion, tagVersion)
