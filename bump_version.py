import os

def bump_version(filename, version): 
    """
    Bumps the version number in the specified file.

    Args:
    filename: The name of the file to update.
    version: The current version number.

    Returns:
    The new version number.
    """

    with open(filename, "r") as f:
        lines = f.readlines()

    for i, line in enumerate(lines):
        if line.startswith("releaseVersion="):
            lines[i] = "releaseVersion=" + str(version)

    with open(filename, "w") as f:
        f.writelines(lines)

    return version + 1

if __name__ == "__main__":
  filename = "version.properties"
  version = 24
  new_version = bump_version(filename, version)

  print("Successfully bumped version to {}".format(new_version))

