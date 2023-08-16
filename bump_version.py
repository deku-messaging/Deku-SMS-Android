import os
import sys

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

    for line in lines:
        if line.startswith("releaseVersion="): 
            releaseVersion = line.split("=")[1].strip() 

        if line.startswith("stagingVersion="): 
            stagingVersion = line.split("=")[1].strip() 

        if line.startswith("nightlyVersion="): 
            nightlyVersion = line.split("=")[1].strip() 

    if releaseVersion is None:
        raise ValueError("Could not find releaseVersion in file")

    if stagingVersion is None:
        raise ValueError("Could not find stagingVersion in file")

    if nightlyVersion is None:
        raise ValueError("Could not find nightlyVersion in file")

    if flavour == "refs/heads/master":
        releaseVersion = int(releaseVersion) + 1
        stagingVersion = 0
        nightlyVersion = 0

    elif flavour == "refs/heads/staging":
        stagingVersion = int(stagingVersion) + 1
        nightlyVersion = 0

    else:
        nightlyVersion = int(nightlyVersion) + 1

    with open(filename, "w") as f:
        f.write("releaseVersion=" + str(releaseVersion) + "\n")
        f.write("stagingVersion=" + str(stagingVersion) + "\n")
        f.write("nightlyVersion=" + str(nightlyVersion))

    return releaseVersion, stagingVersion, nightlyVersion


if __name__ == "__main__":
  filename = "version.properties"

  flavour = sys.argv[1]
  releaseVersion, stagingVersion, nightlyVersion = bump_version(filename, flavour)

  if flavour == "refs/heads/master":
      print(releaseVersion)

  elif flavour == "refs/heads/staging":
      print(stagingVersion)
  
  else:
      print(releaseVersion)

