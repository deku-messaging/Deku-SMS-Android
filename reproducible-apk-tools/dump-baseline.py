#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import dataclasses
import struct
import zipfile
import zlib

from dataclasses import dataclass
from typing import Any, Tuple

# https://android.googlesource.com/platform/tools/base
#   profgen/profgen/src/main/kotlin/com/android/tools/profgen/ArtProfileSerializer.kt

PROF_MAGIC = b"pro\x00"
PROFM_MAGIC = b"prm\x00"

PROF_001_N = b"001\x00"
PROF_005_O = b"005\x00"
PROF_009_O_MR1 = b"009\x00"
PROF_010_P = b"010\x00"
PROF_015_S = b"015\x00"

PROFM_001_N = b"001\x00"
PROFM_002 = b"002\x00"

INLINE_CACHE_MISSING_TYPES_ENCODING = 6
INLINE_CACHE_MEGAMORPHIC_ENCODING = 7

ASSET_PROF = "assets/dexopt/baseline.prof"
ASSET_PROFM = "assets/dexopt/baseline.profm"


class Error(RuntimeError):
    pass


@dataclass(frozen=True)
class ProfHeader:
    num_dex_files: int
    uncompressed_data_size: int
    compressed_data_size: int


@dataclass(frozen=True)
class DexDataHeader:
    profile_key: str
    num_type_ids: int
    hot_method_region_size: int
    dex_checksum: int
    num_method_ids: int


@dataclass(frozen=True)
class MethodInfo:
    method_id: int
    num_inline_caches: int


@dataclass(frozen=True)
class DexDataInfo:
    hot_method_infos: Tuple[MethodInfo, ...]
    type_ids: Tuple[int, ...]
    bitmap_size: int


@dataclass(frozen=True)
class ProfileInfo:
    profile_idx: int
    profile_key: str
    num_type_ids: int
    class_ids: Tuple[int, ...]


def dump_baseline(file: str, verbose: bool = False) -> None:
    with open(file, "rb") as fh:
        _dump_baseline(fh.read(), verbose)


def dump_baseline_apk(apk: str, verbose: bool = False) -> None:
    with zipfile.ZipFile(apk) as zf:
        if ASSET_PROF in zf.namelist():
            print(f"entry={ASSET_PROF}")
            with zf.open(ASSET_PROF) as fh:
                _dump_baseline(fh.read(), verbose)
        if ASSET_PROFM in zf.namelist():
            print(f"entry={ASSET_PROFM}")
            with zf.open(ASSET_PROFM) as fh:
                _dump_baseline(fh.read(), verbose)


# FIXME
# Supported .prof: 010 P
# Unsupported .prof: 001 N, 005 O, 009 O MR1, 015 S
# Supported .profm: 001 N, 002
def _dump_baseline(data: bytes, verbose: bool) -> None:
    magic, data = _split(data, 4)
    version, data = _split(data, 4)
    if magic == PROF_MAGIC:
        if version == PROF_010_P:
            print("prof version=010 P")
            dump_prof(*parse_prof_010_p(data), verbose=verbose)
        else:
            raise Error(f"Unsupported prof version {version!r}")
    elif magic == PROFM_MAGIC:
        if version == PROFM_001_N:
            print("profm version=001 N")
            dump_profm(*parse_profm_001_N(data), verbose=verbose)
        elif version == PROFM_002:
            print("profm version=002")
            dump_profm(*parse_profm_002(data), verbose=verbose)
        else:
            raise Error(f"Unsupported profm version {version!r}")
    else:
        raise Error(f"Unsupported magic {magic!r}")


def dump_prof(header: ProfHeader, dex_data_headers: Tuple[DexDataHeader, ...],
              dex_data_infos: Tuple[DexDataInfo, ...], verbose: bool) -> None:
    print(f"num_dex_files={header.num_dex_files}")
    if verbose:
        print(f"uncompressed_data_size={header.uncompressed_data_size}")
        print(f"compressed_data_size={header.compressed_data_size}")
    for i, h in enumerate(dex_data_headers):
        print(f"dex_data_header {i}")
        print(f"  profile_key={h.profile_key!r}")
        print(f"  num_type_ids={h.num_type_ids}")
        if verbose:
            print(f"  hot_method_region_size={h.hot_method_region_size}")
        print(f"  dex_checksum=0x{h.dex_checksum:x}")
        print(f"  num_method_ids={h.num_method_ids}")
    for i, d in enumerate(dex_data_infos):
        print(f"dex_data {i}")
        print(f"  num_hot_method_ids={len(d.hot_method_infos)}")
        print(f"  num_type_ids={len(d.type_ids)}")
        if verbose:
            for mi in d.hot_method_infos:
                print(f"  method_id={mi.method_id}")
                print(f"  num_inline_caches={mi.num_inline_caches}")
        if verbose:
            for type_id in d.type_ids:
                print(f"  type_id={type_id}")
        print(f"  bitmap_size={d.bitmap_size}")


def dump_profm(header: ProfHeader, profile_infos: Tuple[ProfileInfo, ...],
               verbose: bool) -> None:
    print(f"num_dex_files={header.num_dex_files}")
    if verbose:
        print(f"uncompressed_data_size={header.uncompressed_data_size}")
        print(f"compressed_data_size={header.compressed_data_size}")
    for p in profile_infos:
        print(f"profile_idx={p.profile_idx}")
        print(f"  profile_key={p.profile_key!r}")
        print(f"  num_type_ids={p.num_type_ids}")
        print(f"  num_class_ids={len(p.class_ids)}")
        if verbose:
            for class_id in p.class_ids:
                print(f"  class_id={class_id}")


def parse_prof_010_p(data: bytes) \
        -> Tuple[ProfHeader, Tuple[DexDataHeader, ...], Tuple[DexDataInfo, ...]]:
    num_dex_files, uncompressed_data_size, compressed_data_size, data = _unpack("<BII", data)
    header = ProfHeader(num_dex_files, uncompressed_data_size, compressed_data_size)
    dex_data_headers = []
    dex_data_infos = []
    if len(data) != compressed_data_size:
        raise Error("Compressed data size does not match")
    data = zlib.decompress(data)
    if len(data) != uncompressed_data_size:
        raise Error("Uncompressed data size does not match")
    for i in range(num_dex_files):
        profile_key_size, num_type_ids, hot_method_region_size, \
            dex_checksum, num_method_ids, data = _unpack("<HHIII", data)
        profile_key, data = _split(data, profile_key_size)
        dex_data_headers.append(DexDataHeader(
            profile_key=profile_key.decode(),
            num_type_ids=num_type_ids,
            hot_method_region_size=hot_method_region_size,
            dex_checksum=dex_checksum,
            num_method_ids=num_method_ids,
        ))
    for h in dex_data_headers:
        hot_method_infos = []
        type_ids = []
        region, data = _split(data, h.hot_method_region_size)
        mi_delta = 0
        while region:
            method_id, num_inline_caches, region = _unpack("<HH", region)
            method_id += mi_delta
            mi_delta = method_id
            hot_method_infos.append(MethodInfo(method_id, num_inline_caches))
            # skip inline caches
            region = _skip_inline_caches(PROF_010_P, region, num_inline_caches)
        ti_delta = 0
        for _ in range(h.num_type_ids):
            type_id, data = _unpack("<H", data)
            type_id += ti_delta
            ti_delta = type_id
            type_ids.append(type_id)
        # skip bitmap
        bitmap_size = _bitmap_storage_size(h.num_method_ids)
        _bitmap, data = _split(data, bitmap_size)
        dex_data_infos.append(DexDataInfo(
            hot_method_infos=tuple(hot_method_infos),
            type_ids=tuple(type_ids),
            bitmap_size=bitmap_size,
        ))
    if data:
        raise Error("Expected end of data")
    return header, tuple(dex_data_headers), tuple(dex_data_infos)


def parse_profm_001_N(data: bytes) -> Tuple[ProfHeader, Tuple[ProfileInfo, ...]]:
    num_dex_files, uncompressed_data_size, compressed_data_size, data = _unpack("<BII", data)
    header = ProfHeader(num_dex_files, uncompressed_data_size, compressed_data_size)
    nums_class_ids = []
    profile_infos = []
    if len(data) != compressed_data_size:
        raise Error("Compressed data size does not match")
    data = zlib.decompress(data)
    if len(data) != uncompressed_data_size:
        raise Error("Uncompressed data size does not match")
    if not data:
        return header, ()
    for i in range(num_dex_files):
        class_ids = []
        profile_key_size, num_class_ids, data = _unpack("<HH", data)
        profile_key, data = _split(data, profile_key_size)
        nums_class_ids.append(num_class_ids)
        profile_infos.append(ProfileInfo(
            profile_idx=i,
            profile_key=profile_key.decode(),
            num_type_ids=0,
            class_ids=(),
        ))
    for i in range(num_dex_files):
        ci_delta = 0
        for _ in range(nums_class_ids[i]):
            class_id, data = _unpack("<H", data)
            class_id += ci_delta
            ci_delta = class_id
            class_ids.append(class_id)
        profile_infos[i] = dataclasses.replace(profile_infos[i], class_ids=tuple(class_ids))
    if data:
        raise Error("Expected end of data")
    return header, tuple(profile_infos)


def parse_profm_002(data: bytes) -> Tuple[ProfHeader, Tuple[ProfileInfo, ...]]:
    num_dex_files, uncompressed_data_size, compressed_data_size, data = _unpack("<HII", data)
    header = ProfHeader(num_dex_files, uncompressed_data_size, compressed_data_size)
    profile_infos = []
    if len(data) != compressed_data_size:
        raise Error("Compressed data size does not match")
    data = zlib.decompress(data)
    if len(data) != uncompressed_data_size:
        raise Error("Uncompressed data size does not match")
    for _ in range(num_dex_files):
        class_ids = []
        profile_idx, profile_key_size, data = _unpack("<HH", data)
        profile_key, data = _split(data, profile_key_size)
        num_type_ids, num_class_ids, data = _unpack("<IH", data)
        ci_delta = 0
        for _ in range(num_class_ids):
            class_id, data = _unpack("<H", data)
            class_id += ci_delta
            ci_delta = class_id
            class_ids.append(class_id)
        profile_infos.append(ProfileInfo(
            profile_idx=profile_idx,
            profile_key=profile_key.decode(),
            num_type_ids=num_type_ids,
            class_ids=tuple(class_ids),
        ))
    if data:
        raise Error("Expected end of data")
    return header, tuple(profile_infos)


# FIXME
# Supported: <= 010 P
def _skip_inline_caches(version: bytes, region: bytes, num_inline_caches: int) -> bytes:
    if version <= PROF_010_P:
        for _ in range(num_inline_caches):
            _dex_pc, dex_map_size, region = _unpack("<HB", region)
            if dex_map_size in (INLINE_CACHE_MISSING_TYPES_ENCODING,
                                INLINE_CACHE_MEGAMORPHIC_ENCODING):
                continue
            for _ in range(dex_map_size):
                _dex_profile_idx, num_classes, region = _unpack("<BB", region)
                _, region = _split(region, 2 * num_classes)
        return region
    else:
        raise Error(f"Unsupported version {version!r}")


def _bitmap_storage_size(num_method_ids: int) -> int:
    byte, bits = 8, num_method_ids * 2
    return (bits + byte - 1 & -byte) // byte


def _unpack(fmt: str, data: bytes) -> Any:
    assert all(c in "<BHI" for c in fmt)
    size = fmt.count("B") + 2 * fmt.count("H") + 4 * fmt.count("I")
    return struct.unpack(fmt, data[:size]) + (data[size:],)


def _split(data: bytes, size: int) -> Tuple[bytes, bytes]:
    return data[:size], data[size:]


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(prog="dump-baseline.py")
    parser.add_argument("--apk", action="store_true")
    parser.add_argument("-v", "--verbose", action="store_true")
    parser.add_argument("prof_or_apk", metavar="PROF_OR_APK")
    args = parser.parse_args()
    if args.apk:
        dump_baseline_apk(args.prof_or_apk, args.verbose)
    else:
        dump_baseline(args.prof_or_apk, args.verbose)

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
