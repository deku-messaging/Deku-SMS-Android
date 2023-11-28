#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import struct
import zipfile
import zlib

from fnmatch import fnmatch
from typing import Any, Dict, Tuple

ATTRS = ("compress_type", "create_system", "create_version", "date_time",
         "external_attr", "extract_version", "flag_bits")
LEVELS = (9, 6, 4, 1)


class Error(RuntimeError):
    pass


# FIXME: is there a better alternative?
class ReproducibleZipInfo(zipfile.ZipInfo):
    """Reproducible ZipInfo hack."""

    if "_compresslevel" not in zipfile.ZipInfo.__slots__:   # type: ignore[attr-defined]
        raise Error("zipfile.ZipInfo has no ._compresslevel")

    _compresslevel: int
    _override: Dict[str, Any] = {}

    def __init__(self, zinfo: zipfile.ZipInfo, **override: Any) -> None:
        # pylint: disable=W0231
        if override:
            self._override = {**self._override, **override}
        for k in self.__slots__:
            if hasattr(zinfo, k):
                setattr(self, k, getattr(zinfo, k))

    def __getattribute__(self, name: str) -> Any:
        if name != "_override":
            try:
                return self._override[name]
            except KeyError:
                pass
        return object.__getattribute__(self, name)


def fix_newlines(input_apk: str, output_apk: str, *patterns: str,
                 replace: Tuple[str, str] = ("\n", "\r\n"), verbose: bool = False) -> None:
    if not patterns:
        raise ValueError("No patterns")
    with open(input_apk, "rb") as fh_raw:
        with zipfile.ZipFile(input_apk) as zf_in:
            with zipfile.ZipFile(output_apk, "w") as zf_out:
                for info in zf_in.infolist():
                    attrs = {attr: getattr(info, attr) for attr in ATTRS}
                    zinfo = ReproducibleZipInfo(info, **attrs)
                    if info.compress_type == 8:
                        fh_raw.seek(info.header_offset)
                        n, m = struct.unpack("<HH", fh_raw.read(30)[26:30])
                        fh_raw.seek(info.header_offset + 30 + m + n)
                        ccrc = 0
                        size = info.compress_size
                        while size > 0:
                            ccrc = zlib.crc32(fh_raw.read(min(size, 4096)), ccrc)
                            size -= 4096
                        with zf_in.open(info) as fh_in:
                            comps = {lvl: zlib.compressobj(lvl, 8, -15) for lvl in LEVELS}
                            ccrcs = {lvl: 0 for lvl in LEVELS}
                            while True:
                                data = fh_in.read(4096)
                                if not data:
                                    break
                                for lvl in LEVELS:
                                    ccrcs[lvl] = zlib.crc32(comps[lvl].compress(data), ccrcs[lvl])
                            for lvl in LEVELS:
                                if ccrc == zlib.crc32(comps[lvl].flush(), ccrcs[lvl]):
                                    zinfo._compresslevel = lvl
                                    break
                            else:
                                raise Error(f"Unable to determine compresslevel for {info.filename!r}")
                    elif info.compress_type != 0:
                        raise Error(f"Unsupported compress_type {info.compress_type}")
                    if fnmatches_with_negation(info.filename, *patterns):
                        print(f"fixing {info.filename!r}...")
                        zf_out.writestr(zinfo, replace_newlines(zf_in.read(info).decode(), *replace))
                    else:
                        if verbose:
                            print(f"copying {info.filename!r}...")
                        with zf_in.open(info) as fh_in:
                            with zf_out.open(zinfo, "w") as fh_out:
                                while True:
                                    data = fh_in.read(4096)
                                    if not data:
                                        break
                                    fh_out.write(data)


def replace_newlines(s: str, old: str, new: str) -> str:
    r"""
    Replace old line end with new (unless already present).

    >>> lf, crlf = "\n", "\r\n"
    >>> replace_newlines("foo", lf, crlf)
    'foo'
    >>> replace_newlines("foo\nbar", lf, crlf)
    'foo\r\nbar'
    >>> replace_newlines("foo\nbar\n", lf, crlf)
    'foo\r\nbar\r\n'
    >>> replace_newlines("foo\nbar\r\n", lf, crlf)
    'foo\r\nbar\r\n'
    >>> replace_newlines("foo", crlf, lf)
    'foo'
    >>> replace_newlines("foo\nbar", crlf, lf)
    'foo\nbar'
    >>> replace_newlines("foo\r\nbar", crlf, lf)
    'foo\nbar'
    >>> replace_newlines("foo\r\nbar\r\n", crlf, lf)
    'foo\nbar\n'

    """
    result, overlap, n = [], old.endswith(new), len(old)
    for line in s.splitlines(True):
        if line.endswith(old) and (overlap or not line.endswith(new)):
            line = line[:-n] + new
        result.append(line)
    return "".join(result)


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
    parser = argparse.ArgumentParser(prog="fix-newlines.py")
    parser.add_argument("--from-crlf", action="store_true")
    parser.add_argument("--to-crlf", dest="from_crlf", action="store_false")
    parser.add_argument("-v", "--verbose", action="store_true")
    parser.add_argument("input_apk", metavar="INPUT_APK")
    parser.add_argument("output_apk", metavar="OUTPUT_APK")
    parser.add_argument("patterns", metavar="PATTERN", nargs="+")
    args = parser.parse_args()
    replace = ("\r\n", "\n") if args.from_crlf else ("\n", "\r\n")
    fix_newlines(args.input_apk, args.output_apk, *args.patterns,
                 replace=replace, verbose=args.verbose)

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
