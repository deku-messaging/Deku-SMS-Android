#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2022 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import os
import struct
import zipfile

from collections import namedtuple
from typing import BinaryIO

ZipData = namedtuple("ZipData", ("cd_offset", "eocd_offset", "cd_and_eocd"))


class Error(RuntimeError):
    pass


def sort_apk(input_apk: str, output_apk: str, *, realign: bool = True,
             force_align: bool = True, reset_lh_extra: bool = False) -> None:
    with zipfile.ZipFile(input_apk, "r") as zf:
        infos = zf.infolist()
    zdata = zip_data(input_apk)
    offsets = {}
    with open(input_apk, "rb") as fhi, open(output_apk, "w+b") as fho:
        for info in sorted(infos, key=lambda info: info.filename):
            fhi.seek(info.header_offset)
            hdr = fhi.read(30)
            if hdr[:4] != b"\x50\x4b\x03\x04":
                raise Error("Expected local file header signature")
            n, m = struct.unpack("<HH", hdr[26:30])
            hdr += fhi.read(n + m)
            if info.filename in offsets:
                raise Error(f"Duplicate ZIP entry: {info.filename!r}")
            offsets[info.filename] = off_o = fho.tell()
            m_old = m
            if reset_lh_extra:
                m = len(info.extra)
                m_b = int.to_bytes(m, 2, "little")
                hdr = hdr[:28] + m_b + hdr[30:30 + n] + info.extra
            if realign and info.compress_type == 0:
                hdr = _realign_zip_entry(info, hdr, n, m_old, m, off_o, force=force_align)
            fho.write(hdr)
            _copy_bytes(fhi, fho, info.compress_size)
            if info.flag_bits & 0x08:
                data_descriptor = fhi.read(12)
                if data_descriptor[:4] == b"\x50\x4b\x07\x08":
                    data_descriptor += fhi.read(4)
                fho.write(data_descriptor)
        fhi.seek(zdata.cd_offset)
        cd_offset = fho.tell()
        hdrs = []
        for info in infos:
            hdr = fhi.read(46)
            if hdr[:4] != b"\x50\x4b\x01\x02":
                raise Error("Expected central directory file header signature")
            n, m, k = struct.unpack("<HHH", hdr[28:34])
            hdr += fhi.read(n + m + k)
            off = int.to_bytes(offsets[info.filename], 4, "little")
            hdr = hdr[:42] + off + hdr[46:]
            hdrs.append((info.filename, hdr))
        for _, hdr in sorted(hdrs, key=lambda x: x[0]):
            fho.write(hdr)
        eocd_offset = fho.tell()
        fho.write(zdata.cd_and_eocd[zdata.eocd_offset - zdata.cd_offset:])
        fho.seek(eocd_offset + 8)
        fho.write(struct.pack("<HHLL", len(offsets), len(offsets),
                              eocd_offset - cd_offset, cd_offset))


# FIXME
# NB: doesn't sync local & CD headers!
def _realign_zip_entry(info: zipfile.ZipInfo, hdr: bytes, n: int, m_old: int,
                       m: int, off_o: int, *, force: bool = False) -> bytes:
    align = 4096 if info.filename.endswith(".so") else 4
    old_off = 30 + n + m_old + info.header_offset
    new_off = 30 + n + m + off_o
    old_xtr = hdr[30 + n:30 + n + m]
    new_xtr = b""
    while len(old_xtr) >= 4:
        hdr_id, size = struct.unpack("<HH", old_xtr[:4])
        if size > len(old_xtr) - 4:
            break
        if not (hdr_id == 0 and size == 0):
            if hdr_id == 0xd935:
                if size >= 2:
                    align = int.from_bytes(old_xtr[4:6], "little")
            else:
                new_xtr += old_xtr[:size + 4]
        old_xtr = old_xtr[size + 4:]
    if old_off % align == 0 and (force or new_off % align != 0):
        pad = (align - (new_off - m + len(new_xtr) + 6) % align) % align
        xtr = new_xtr + struct.pack("<HHH", 0xd935, 2 + pad, align) + pad * b"\x00"
        m_b = int.to_bytes(len(xtr), 2, "little")
        hdr = hdr[:28] + m_b + hdr[30:30 + n] + xtr
    return hdr


def _copy_bytes(fhi: BinaryIO, fho: BinaryIO, size: int, blocksize: int = 4096) -> None:
    while size > 0:
        data = fhi.read(min(size, blocksize))
        if not data:
            break
        size -= len(data)
        fho.write(data)
    if size != 0:
        raise Error("Unexpected EOF")


def zip_data(apkfile: str, count: int = 1024) -> ZipData:
    with open(apkfile, "rb") as fh:
        fh.seek(-count, os.SEEK_END)
        data = fh.read()
        pos = data.rfind(b"\x50\x4b\x05\x06")
        if pos == -1:
            raise Error("Expected end of central directory record (EOCD)")
        fh.seek(pos - len(data), os.SEEK_CUR)
        eocd_offset = fh.tell()
        fh.seek(16, os.SEEK_CUR)
        cd_offset = int.from_bytes(fh.read(4), "little")
        fh.seek(cd_offset)
        cd_and_eocd = fh.read()
    return ZipData(cd_offset, eocd_offset, cd_and_eocd)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(prog="sort-apk.py")
    parser.add_argument("--no-realign", dest="realign", action="store_false")
    parser.add_argument("--no-force-align", dest="force_align", action="store_false")
    parser.add_argument("--reset-lh-extra", dest="reset_lh_extra", action="store_true")
    parser.add_argument("input_apk", metavar="INPUT_APK")
    parser.add_argument("output_apk", metavar="OUTPUT_APK")
    args = parser.parse_args()
    sort_apk(args.input_apk, args.output_apk, realign=args.realign,
             force_align=args.force_align, reset_lh_extra=args.reset_lh_extra)

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
