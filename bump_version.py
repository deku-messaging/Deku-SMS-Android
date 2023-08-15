import os

def bump_version(filename):
    """
    Bumps the version number in the specified file.

    Args:
    filename: The name of the file to update.

    Returns:
    The new version number.
    """

    with open(filename, "r") as f:
        lines = f.readlines()

    version = None
    for line in lines:
        if line.startswith("releaseVersion="): 
            version = line.split("=")[1].strip() 
            break

    if version is None:
        raise ValueError("Could not find releaseVersion in file")

    new_version = int(version) + 1

    with open(filename, "w") as f:
        f.write("releaseVersion=" + str(new_version))

    return new_version


if __name__ == "__main__":
  filename = "version.properties"
  new_version = bump_version(filename)

  print("Successfully bumped version to {}".format(new_version))

