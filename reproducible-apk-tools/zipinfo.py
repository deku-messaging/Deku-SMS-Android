#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import calendar
import os
import stat
import struct
import time
import zipfile

from dataclasses import dataclass
from typing import Callable, Optional

# FIXME: support more types, needs test cases
# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L1887
SYS_FAT, SYS_UNX, SYS_NTF = (0, 3, 11)
SYSTEM = {SYS_FAT: "fat", SYS_UNX: "unx", SYS_NTF: "ntf"}

# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L2086
EXE_EXTS = {"com", "exe", "btm", "cmd", "bat"}

# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L1896
COMPRESS_TYPE = {
    zipfile.ZIP_STORED: "stor",
    zipfile.ZIP_DEFLATED: "def",  # DEFLATE_TYPE char is appended
    zipfile.ZIP_BZIP2: "bzp2",
    zipfile.ZIP_LZMA: "lzma",
}

# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L1886
# normal, maximum, fast, superfast
DEFLATE_TYPE = "NXFS"

EXTRA_DATA_INFO = {
    # extra, data descriptor
    (False, False): "-",
    (False, True): "l",
    (True, False): "x",
    (True, True): "X",
}


@dataclass(frozen=True)
class Time:
    """Unix time from extra field (UT or UX)."""
    mtime: int
    atime: Optional[int]
    ctime: Optional[int]


class Error(RuntimeError):
    pass


# FIXME
# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L1097
# https://sources.debian.org/src/zip/3.0-12/zip.h/#L211
def format_info(info: zipfile.ZipInfo, *, extended: bool = False,
                long: bool = False) -> str:
    r"""
    Format ZIP entry info.

    >>> zf = zipfile.ZipFile("test/data/crlf.apk")
    >>> info1 = zf.getinfo("META-INF/")
    >>> info2 = zf.getinfo("resources.arsc")
    >>> info3 = zf.getinfo("LICENSE.GPLv3")
    >>> format_info(info1)
    '-rw----     2.0 fat        0 bx defN 17-May-15 11:25 META-INF/'
    >>> format_info(info1, extended=True)
    'drw----     2.0 fat        0 bx        2 defN 2017-05-15 11:25:18 00000000 META-INF/'
    >>> format_info(info2)
    '-rw----     1.0 fat      896 b- stor 09-Jan-01 00:00 resources.arsc'
    >>> format_info(info3)
    '-rw-------  3.0 unx    35823 t- defN 80-Jan-01 00:00 LICENSE.GPLv3'
    >>> info3.external_attr |= 0o644 << 16
    >>> format_info(info3, long=True)
    '-rw-r--r--  3.0 unx    35823 t-    12289 defN 80-Jan-01 00:00 LICENSE.GPLv3'
    >>> format_info(info3, extended=True)
    '-rw-r--r--  3.0 unx    35823 t-    12289 defN 1980-01-01 00:00:00 cece3b93 LICENSE.GPLv3'
    >>> info4 = zipfile.ZipInfo("foo\n.com")
    >>> info4.file_size = info4.compress_size = 0
    >>> format_info(info4)
    '-rwx---     2.0 unx        0 b- stor 80-Jan-01 00:00 foo^J.com'
    >>> info4.extra = b'UT\x05\x00\x01\x00\x00\x00\x00'
    >>> format_info(info4)
    '-rwx---     2.0 unx        0 bx stor 70-Jan-01 00:00 foo^J.com'
    >>> info4.extra = b'UX\x08\x00\x80h\x9e>|h\x9e>'
    >>> format_info(info4)
    '-rwx---     2.0 unx        0 bx stor 03-Apr-17 08:40 foo^J.com'

    """
    if t := extra_field_time(info.extra):
        date_time = tuple(time.localtime(t.mtime))[:6]
    else:
        date_time = info.date_time
    perm = format_permissions(info)
    if extended and info.filename.endswith("/"):
        perm = "d" + perm[1:]                       # directory
    vers = "{}.{}".format(info.create_version // 10,
                          info.create_version % 10)
    syst = SYSTEM.get(info.create_system, "???")
    xinf = "t" if info.internal_attr == 1 else "b"  # text/binary
    if info.flag_bits & 1:
        xinf = xinf.upper()                         # encrypted
    xinf += EXTRA_DATA_INFO[(bool(info.extra), bool(info.flag_bits & 0x08))]
    comp = COMPRESS_TYPE.get(info.compress_type, "????")
    if info.compress_type == zipfile.ZIP_DEFLATED:
        comp += DEFLATE_TYPE[(info.flag_bits >> 1) & 3]
    if extended:
        dt = "{}-{:02d}-{:02d}".format(*date_time[:3])
        tm = "{:02d}:{:02d}:{:02d}".format(*date_time[3:])
    else:
        dt = "{:02d}-{}-{:02d}".format(
            date_time[0] % 100,
            calendar.month_abbr[date_time[1]] or "000",
            date_time[2]
        )
        tm = "{:02d}:{:02d}".format(*date_time[3:5])
    fields = [f"{perm:<11}", vers, syst, f"{info.file_size:>8}", xinf]
    if long or extended:
        fields.append(f"{info.compress_size:>8}")
    fields += [comp, dt, tm]
    if extended:
        fields.append(f"{info.CRC:08x}")
    fields.append(printable_filename(info.filename))
    return " ".join(fields)


# FIXME
# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L2064
def format_permissions(info: zipfile.ZipInfo) -> str:
    """
    Format ZIP entry Unix or FAT permissions.

    >>> zf = zipfile.ZipFile("test/data/crlf.apk")
    >>> info1 = zf.getinfo("META-INF/")
    >>> info2 = zf.getinfo("resources.arsc")
    >>> info3 = zf.getinfo("LICENSE.GPLv3")
    >>> format_permissions(info1)
    '-rw----'
    >>> format_permissions(info2)
    '-rw----'
    >>> format_permissions(info3)
    '-rw-------'
    >>> info3.external_attr = info3.external_attr | (0o644 << 16)
    >>> format_permissions(info3)
    '-rw-r--r--'
    >>> info4 = zipfile.ZipInfo("foo.com")
    >>> format_permissions(info4)
    '-rwx---'

    """
    hi = info.external_attr >> 16
    if hi and info.create_system in (SYS_UNX, SYS_FAT):
        return stat.filemode(hi)
    exe = os.path.splitext(info.filename)[1][1:].lower() in EXE_EXTS
    xat = info.external_attr & 0xFF
    return "".join((
        'd' if xat & 0x10 else '-',
        'r',
        '-' if xat & 0x01 else 'w',
        'x' if xat & 0x10 or exe else '-',
        'a' if xat & 0x20 else '-',
        'h' if xat & 0x02 else '-',
        's' if xat & 0x04 else '-',
    ))


# https://sources.debian.org/src/zip/3.0-12/zip.h/#L217
# https://sources.debian.org/src/zip/3.0-12/zipfile.c/#L6544
def extra_field_time(extra: bytes, local: bool = False) -> Optional[Time]:
    r"""
    Get unix time from extra field (UT or UX).

    >>> t = extra_field_time(b'UT\x05\x00\x01\x00\x00\x00\x00')
    >>> t
    Time(mtime=0, atime=None, ctime=None)
    >>> tuple(time.localtime(t.mtime))
    (1970, 1, 1, 0, 0, 0, 3, 1, 0)
    >>> t = extra_field_time(b'UT\x05\x00\x01\xda\xe9\xe36')
    >>> t
    Time(mtime=920906202, atime=None, ctime=None)
    >>> tuple(time.localtime(t.mtime))
    (1999, 3, 8, 15, 16, 42, 0, 67, 0)
    >>> t = extra_field_time(b'UX\x08\x00\x80h\x9e>|h\x9e>')
    >>> t
    Time(mtime=1050568828, atime=1050568832, ctime=None)
    >>> tuple(time.localtime(t.mtime))
    (2003, 4, 17, 8, 40, 28, 3, 107, 0)
    >>> tuple(time.localtime(t.atime))
    (2003, 4, 17, 8, 40, 32, 3, 107, 0)
    >>> t = extra_field_time(b'UX\x08\x00\xda\xe9\xe36\xda\xe9\xe36')
    >>> t
    Time(mtime=920906202, atime=920906202, ctime=None)
    >>> tuple(time.localtime(t.mtime))
    (1999, 3, 8, 15, 16, 42, 0, 67, 0)
    >>> tuple(time.localtime(t.atime))
    (1999, 3, 8, 15, 16, 42, 0, 67, 0)

    """
    while len(extra) >= 4:
        hdr_id, size = struct.unpack("<HH", extra[:4])
        if size > len(extra) - 4:
            break
        if hdr_id == 0x5455 and size >= 1:
            flags = extra[4]
            if flags & 0x1 and size >= 5:
                mtime = int.from_bytes(extra[5:9], "little")
                atime = ctime = None
                if local:
                    if flags & 0x2 and size >= 9:
                        atime = int.from_bytes(extra[9:13], "little")
                    if flags & 0x4 and size >= 13:
                        ctime = int.from_bytes(extra[13:17], "little")
                return Time(mtime, atime, ctime)
        elif hdr_id == 0x5855 and size >= 8:
            atime = int.from_bytes(extra[4:8], "little")
            mtime = int.from_bytes(extra[8:12], "little")
            return Time(mtime, atime, None)
        extra = extra[size + 4:]
    return None


def printable_filename(s: str) -> str:
    r"""
    Replace ASCII control characters with caret notation (e.g. ^M, ^J) and other
    non-printable characters with backslash escapes like repr().

    >>> printable_filename("foo bar.baz")
    'foo bar.baz'
    >>> printable_filename("foo\r\n\x7fbar\x82\u0fff.baz")
    'foo^M^J^?bar\\x82\\u0fff.baz'

    """
    t = []
    for c in s:
        if c.isprintable():
            t.append(c)
        elif (i := ord(c)) in range(32):
            t.append("^" + chr(i + 64))
        elif i == 127:
            t.append("^?")
        else:
            t.append(repr(c)[1:-1])
    return "".join(t)


def zipinfo(zip_file: str, *, extended: bool = False, long: bool = False,
            sort_by_offset: bool = False, fmt: Callable[..., str] = format_info) -> None:
    """List ZIP entries, like Info-ZIP's zipinfo(1)."""
    with zipfile.ZipFile(zip_file) as zf:
        if sort_by_offset:
            infos = sorted(zf.infolist(), key=lambda i: i.header_offset)
        else:
            infos = zf.infolist()
        size = os.path.getsize(zip_file)
        tot_u = tot_c = 0
        print(f"Archive:  {printable_filename(zip_file)}")
        print(f"Zip file size: {size} bytes, number of entries: {len(infos)}")
        if infos:
            for info in infos:
                tot_u += info.file_size
                tot_c += info.compress_size
                print(fmt(info, extended=extended, long=long))
                if info.flag_bits & 1:  # encrypted
                    tot_c -= 12         # don't count extra 12 header bytes
            s = "" if len(infos) == 1 else "s"
            r = _cfactor(tot_u, tot_c)
            print(f"{len(infos)} file{s}, {tot_u} bytes uncompressed, "
                  f"{tot_c} bytes compressed:  {r}")
        else:
            print("Empty zipfile.")


# https://sources.debian.org/src/unzip/6.0-27/list.c/#L708
# https://sources.debian.org/src/unzip/6.0-27/zipinfo.c/#L913
def _cfactor(u: int, c: int) -> str:
    """
    Compression factor as reported by Info-ZIP's zipinfo(1).

    >>> _cfactor(0, 0)
    '0.0%'
    >>> _cfactor(400, 300)
    '25.0%'
    >>> _cfactor(300, 400)
    '-33.3%'
    >>> _cfactor(84624808, 25825672)
    '69.5%'
    >>> _cfactor(938, 492)
    '47.5%'
    >>> _cfactor(3070914, 1205461)
    '60.8%'

    """
    if not u:
        r, s = 0, ""
    else:
        f, d = (1, u // 1000) if u > 2000000 else (1000, u)
        if u >= c:
            r, s = (f * (u - c) + (d >> 1)) // d, ""
        else:
            r, s = (f * (c - u) + (d >> 1)) // d, "-"
    return f"{s}{r//10}.{r%10}%"


def zip_filenames(zip_file: str, *, sort_by_offset: bool = False) -> None:
    """List ZIP entry filenames, one per line."""
    with zipfile.ZipFile(zip_file) as zf:
        if sort_by_offset:
            infos = sorted(zf.infolist(), key=lambda i: i.header_offset)
        else:
            infos = zf.infolist()
        for info in infos:
            print(printable_filename(info.filename))


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(prog="zipinfo.py")
    parser.add_argument("-1", "--filenames-only", action="store_true",
                        help="only print filenames, one per line")
    parser.add_argument("-e", "--extended", action="store_true",
                        help="use extended output format")
    parser.add_argument("-l", "--long", action="store_true",
                        help="use long output format")
    parser.add_argument("--sort-by-offset", action="store_true",
                        help="sort entries by header offset")
    parser.add_argument("zipfile", metavar="ZIPFILE")
    args = parser.parse_args()
    try:
        if args.filenames_only and not (args.extended or args.long):
            zip_filenames(args.zipfile, sort_by_offset=args.sort_by_offset)
        else:
            zipinfo(args.zipfile, extended=args.extended, long=args.long,
                    sort_by_offset=args.sort_by_offset)
    except BrokenPipeError:
        pass

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
