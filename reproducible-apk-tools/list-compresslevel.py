#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import struct
import zipfile
import zlib

from fnmatch import fnmatch

LEVELS = (9, 6, 4, 1)


class Error(RuntimeError):
    pass


def list_compresslevel(apk: str, *patterns: str) -> None:
    with open(apk, "rb") as fh_raw:
        with zipfile.ZipFile(apk) as zf:
            for info in zf.infolist():
                if patterns and not fnmatches_with_negation(info.filename, *patterns):
                    continue
                levels = []
                if info.compress_type == 8:
                    fh_raw.seek(info.header_offset)
                    n, m = struct.unpack("<HH", fh_raw.read(30)[26:30])
                    fh_raw.seek(info.header_offset + 30 + m + n)
                    ccrc = 0
                    size = info.compress_size
                    while size > 0:
                        ccrc = zlib.crc32(fh_raw.read(min(size, 4096)), ccrc)
                        size -= 4096
                    with zf.open(info) as fh:
                        comps = {lvl: zlib.compressobj(lvl, 8, -15) for lvl in LEVELS}
                        ccrcs = {lvl: 0 for lvl in LEVELS}
                        while True:
                            data = fh.read(4096)
                            if not data:
                                break
                            for lvl in LEVELS:
                                ccrcs[lvl] = zlib.crc32(comps[lvl].compress(data), ccrcs[lvl])
                        for lvl in LEVELS:
                            if ccrc == zlib.crc32(comps[lvl].flush(), ccrcs[lvl]):
                                levels.append(lvl)
                        if not levels:
                            raise Error(f"Unable to determine compresslevel for {info.filename!r}")
                elif info.compress_type != 0:
                    raise Error(f"Unsupported compress_type {info.compress_type}")
                result = "|".join(map(str, levels)) if levels else None
                print(f"filename={info.filename!r} compresslevel={result}")


def fnmatches_with_negation(filename: str, *patterns: str) -> bool:
    r"""
    Filename matching with shell patterns and negation.

    Checks whether filename matches any of the fnmatch patterns.

    An optional prefix "!" negates the pattern, invalidating a successful match
    by any preceding pattern; use a backslash ("\") in front of the first "!"
    for patterns that begin with a literal "!".

    >>> fnmatches_with_negation("foo.xml", "*", "!*.png")
    True
    >>> fnmatches_with_negation("foo.png", "*", "!*.png")
    False
    >>> fnmatches_with_negation("!foo.png", r"\!*.png")
    True

    """
    matches = False
    for p in patterns:
        if p.startswith("!"):
            if fnmatch(filename, p[1:]):
                matches = False
        else:
            if p.startswith(r"\!"):
                p = p[1:]
            if fnmatch(filename, p):
                matches = True
    return matches


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(prog="list-compresslevel.py")
    parser.add_argument("apk", metavar="APK")
    parser.add_argument("patterns", metavar="PATTERN", nargs="*")
    args = parser.parse_args()
    try:
        list_compresslevel(args.apk, *args.patterns)
    except BrokenPipeError:
        pass

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
