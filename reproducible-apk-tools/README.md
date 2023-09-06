<!-- SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net> -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

[![GitHub Release](https://img.shields.io/github/release/obfusk/reproducible-apk-tools.svg?logo=github)](https://github.com/obfusk/reproducible-apk-tools/releases)
[![PyPI Version](https://img.shields.io/pypi/v/repro-apk.svg)](https://pypi.python.org/pypi/repro-apk)
[![Python Versions](https://img.shields.io/pypi/pyversions/repro-apk.svg)](https://pypi.python.org/pypi/repro-apk)
[![CI](https://github.com/obfusk/reproducible-apk-tools/workflows/CI/badge.svg)](https://github.com/obfusk/reproducible-apk-tools/actions?query=workflow%3ACI)
[![GPLv3+](https://img.shields.io/badge/license-GPLv3+-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

<!--
<a href="https://repology.org/project/repro-apk/versions">
  <img src="https://repology.org/badge/vertical-allrepos/repro-apk.svg?header="
    alt="Packaging status" align="right" />
</a>

<a href="https://repology.org/project/python:repro-apk/versions">
  <img src="https://repology.org/badge/vertical-allrepos/python:repro-apk.svg?header="
    alt="Packaging status" align="right" />
</a>
-->

# reproducible-apk-tools

[`fix-compresslevel.py`](#fix-compresslevelpy),
[`fix-files.py`](#fix-filespy),
[`fix-newlines.py`](#fix-newlinespy),
[`rm-files.py`](#rm-filespy),
[`sort-apk.py`](#sort-apkpy),
[`sort-baseline.py`](#sort-baselinepy),
[`zipalign.py`](#zipalignpy);

[`diff-zip-meta.py`](#diff-zip-metapy),
[`dump-arsc.py`](#dump-arscpy),
[`dump-axml.py`](#dump-axmlpy),
[`dump-baseline.py`](#dump-baselinepy),
[`list-compresslevel.py`](#list-compresslevelpy),
[`zipinfo.py`](#zipinfopy);

[`inplace-fix.py`](#inplace-fixpy).

## scripts to make android apks reproducible

`reproducible-apk-tools` is a collection of scripts (available as subcommands of
the `repro-apk` command) to help make APKs reproducible (e.g. by changing line
endings from LF to CRLF), or find out why they are not (e.g. by comparing ZIP
file metadata, or dumping `baseline.prof` files).

### fix-compresslevel.py

Recompress with different compression level.

Specify which files to change by providing at least one fnmatch-style pattern,
e.g. `'assets/foo/*.bar'`.

If two APKs have identical contents but some ZIP entries are compressed with a
different compression level, thus making the APKs not bit-by-bit identical, this
script may help.

```bash
$ fix-compresslevel.py --help
usage: fix-compresslevel.py [-h] [-v] INPUT_APK OUTPUT_APK COMPRESSLEVEL PATTERN [PATTERN ...]
[...]
$ apksigcopier compare signed.apk --unsigned unsigned.apk
DOES NOT VERIFY
[...]
$ fix-compresslevel.py unsigned.apk fixed.apk 6 assets/foo/bar.js
fixing 'assets/foo/bar.js'...
$ zipalign -f 4 fixed.apk fixed-aligned.apk
$ apksigcopier compare signed.apk --unsigned fixed-aligned.apk && echo OK
OK
```

NB: this builds a new ZIP file, preserving most ZIP metadata (and recompressing
entries not matching the pattern using the same compression level as in the
original APK) but not everything: e.g. copying the existing local header extra
fields which contain padding for alignment is not supported by Python's
`ZipFile`, which is why `zipalign` is usually needed.

### fix-files.py

Process ZIP entries using an external command.

Runs the command for each specified file, providing the old file contents as
stdin and using stdout as the new file contents.

The provided command is split on whitespace to allow passing arguments (e.g.
`'foo --bar'`), but shell syntax is not supported.

Specify which files to process by providing at least one fnmatch-style pattern,
e.g. `'META-INF/services/*'`.

```bash
$ fix-files.py --help
usage: fix-files.py [-h] [-v] INPUT_APK OUTPUT_APK COMMAND PATTERN [PATTERN ...]
[...]
$ apksigcopier compare signed.apk --unsigned unsigned.apk
DOES NOT VERIFY
[...]
$ fix-files.py unsigned.apk fixed.apk unix2dos 'META-INF/services/*'
processing 'META-INF/services/foo' with 'unix2dos'...
processing 'META-INF/services/bar' with 'unix2dos'...
$ zipalign -f 4 fixed.apk fixed-aligned.apk
$ apksigcopier compare signed.apk --unsigned fixed-aligned.apk && echo OK
OK
```

NB: this builds a new ZIP file, preserving most ZIP metadata (and recompressing
using the same compression level) but not everything: e.g. copying the existing
local header extra fields which contain padding for alignment is not supported
by Python's `ZipFile`, which is why `zipalign` is usually needed.

### fix-newlines.py

Change line endings from LF to CRLF (or vice versa w/ `--from-crlf`).

Specify which files to change by providing at least one fnmatch-style pattern,
e.g. `'META-INF/services/*'`.

If the signed APK was built on Windows and has e.g. `META-INF/services/` files
with CRLF line endings whereas the unsigned APK was build on Linux/macOS and has
LF line endings, this script may help.

```bash
$ fix-newlines.py --help
usage: fix-newlines.py [-h] [--from-crlf] [--to-crlf] [-v] INPUT_APK OUTPUT_APK PATTERN [PATTERN ...]
[...]
$ apksigcopier compare signed.apk --unsigned unsigned.apk
DOES NOT VERIFY
[...]
$ fix-newlines.py unsigned.apk fixed.apk 'META-INF/services/*'
fixing 'META-INF/services/foo'...
fixing 'META-INF/services/bar'...
$ zipalign -f 4 fixed.apk fixed-aligned.apk
$ apksigcopier compare signed.apk --unsigned fixed-aligned.apk && echo OK
OK
```

NB: this builds a new ZIP file, preserving most ZIP metadata (and recompressing
using the same compression level) but not everything: e.g. copying the existing
local header extra fields which contain padding for alignment is not supported
by Python's `ZipFile`, which is why `zipalign` is usually needed.

### rm-files.py

Remove entries from ZIP file.

Specify which files to remove by providing at least one fnmatch-style pattern,
e.g. `'META-INF/MANIFEST.MF'`.

```bash
$ rm-files.py --help
usage: rm-files.py [-h] [-v] INPUT_APK OUTPUT_APK PATTERN [PATTERN ...]
[...]
$ rm-files.py some.apk fixed.apk META-INF/MANIFEST.IN
skipping 'META-INF/MANIFEST.IN'...
$ zipalign -f 4 fixed.apk fixed-aligned.apk
```

NB: this builds a new ZIP file, preserving most ZIP metadata (and recompressing
using the same compression level) but not everything: e.g. copying the existing
local header extra fields which contain padding for alignment is not supported
by Python's `ZipFile`, which is why `zipalign` is usually needed.

### sort-apk.py

Sort (and w/o `--no-realign` also realign) the ZIP entries of an APK.

If the ordering of the ZIP entries in an APK is not deterministic/reproducible,
this script may help.  You'll almost certainly need to use it for all builds
though, since it can only sort the APK, not recreate a different ordering that
is deterministic but not sorted; see also the alignment CAVEAT.

```bash
$ sort-apk.py --help
usage: sort-apk.py [-h] [--no-realign] [--no-force-align] [--reset-lh-extra] INPUT_APK OUTPUT_APK
[...]
$ unzip -l unsigned.apk
Archive:  unsigned.apk
  Length      Date    Time    Name
---------  ---------- -----   ----
        6  2017-05-15 11:24   lib/armeabi/fake.so
     1672  2009-01-01 00:00   AndroidManifest.xml
      896  2009-01-01 00:00   resources.arsc
     1536  2009-01-01 00:00   classes.dex
---------                     -------
     4110                     4 files
$ sort-apk.py unsigned.apk sorted.apk
$ unzip -l sorted.apk
Archive:  sorted.apk
  Length      Date    Time    Name
---------  ---------- -----   ----
     1672  2009-01-01 00:00   AndroidManifest.xml
     1536  2009-01-01 00:00   classes.dex
        6  2017-05-15 11:24   lib/armeabi/fake.so
      896  2009-01-01 00:00   resources.arsc
---------                     -------
     4110                     4 files
```

NB: this directly copies the (bytes of the) original ZIP entries from the
original file, thus preserving all ZIP metadata.

#### CAVEAT: alignment

Unfortunately, the padding added to ZIP local header extra fields for alignment
makes it hard to make sorting deterministic: unless the original APK was not
aligned at all, the padding is often different when the APK entries had a
different order (and thus a different offset) before sorting.

Because of this, `sort-apk` forcefully recreates the padding even if the entry
is already aligned (since that doesn't mean the padding is identical) to make
its output as deterministic as possible.  The downside is that it'll often add
"unnecessary" 8-byte padding to entries that didn't need alignment.

You can disable this using `--no-force-align`, or skip realignment completely
using `--no-realign`.  If you're certain you don't need to keep the old values,
you can also choose to reset the local header extra fields to the values from
the central directory entries with `--reset-lh-extra`.

If you use `--reset-lh-extra`, you'll probably want to combine it with either
`--no-force-align` (which should prevent the "unnecessary" 8-byte padding) or
`--no-realign` + `zipalign` (which uses smaller padding).

NB: the alignment padding used by `sort-apk` is the same as that used by
`apksigner` (a `0xd935` "Android ZIP Alignment Extra Field" which stores the
alignment itself plus zero padding and is thus always at least 6 bytes), whereas
`zipalign` just uses plain zero padding.

### sort-baseline.py

Sort `baseline.profm` (extracted or inside an APK).

```bash
$ sort-baseline.py --help
usage: sort-baseline.py [-h] [--apk] INPUT_PROF_OR_APK OUTPUT_PROF_OR_APK
[...]
$ diff -qs a/baseline.profm b/baseline.profm
Files a/baseline.profm and b/baseline.profm differ
$ sort-baseline.py a/baseline.profm a/baseline-sorted.profm
$ sort-baseline.py b/baseline.profm b/baseline-sorted.profm
$ diff -qs a/baseline-sorted.profm b/baseline-sorted.profm
Files a/baseline-sorted.profm and b/baseline-sorted.profm are identical
```

```bash
$ sort-baseline.py --apk unsigned.apk sorted-baseline.apk
$ zipalign -f 4 sorted-baseline.apk sorted-baseline-aligned.apk
```

NB: does not support all file format versions yet.

NB: with `--apk`, this builds a new ZIP file, preserving most ZIP metadata (and
recompressing using the same compression level) but not everything: e.g. copying
the existing local header extra fields which contain padding for alignment is
not supported by Python's `ZipFile`, which is why `zipalign` is usually needed.

### zipalign.py

Align uncompressed ZIP/APK entries to 4-byte boundaries (and `.so` shared object
files to 4096-byte boundaries with `-p`/`--page-align`).

This implementation aims for compatibility with Android's `zipalign`, with the
exception of there not being a `-f` option to enable overwriting an existing
output file (it will always be overwritten), and the `ALIGN` parameter -- which
must always be 4 anyway -- being optional; not does it support the `-c`, `-v`,
or `-z` options.

By default, the same plain zero padding as the original `zipalign` is used, but
with the `--pad-like-apksigner` option it uses the same alignment padding as
`apksigner` (a `0xd935` "Android ZIP Alignment Extra Field" which stores the
alignment itself plus zero padding and is thus always at least 6 bytes).

```bash
$ zipalign.py --help
usage: zipalign.py [-h] [-p] [--pad-like-apksigner] [--copy-extra] [--no-update-lfh]
                   [ALIGN] INPUT_APK OUTPUT_APK
[...]
$ zipalign -f 4 fixed.apk fixed-aligned.apk
$ zipalign.py fixed.apk fixed-aligned-py.apk
$ cmp fixed-aligned.apk fixed-aligned-py.apk && echo OK
OK
```

## scripts to dump info from apks and related file formats

### diff-zip-meta.py

Diff ZIP file metadata.

NB: this will not compare the *contents* of the ZIP entries, only metadata and
other non-contents bytes; to compare the contents of ZIP/APK files, use e.g.
[`diffoscope`](https://diffoscope.org).

This will show differences in filenames, central directory headers, local file
headers, data descriptors, entry sizes, etc.

Additional tests include compression level (if it can be determined), CRC32
checksum of compressed data, and extra data before entries or the central
directory; you can skip these (relatively slow) tests using `--no-additional`.

Some differences make the output quite verbose and/or are usually the result of
other differences; you can skip/ignore these using `--no-lfh-extra`,
`--no-offsets`, `--no-ordering`.

```bash
$ diff-zip-meta.py --help
usage: diff-zip-meta.py [-h] [--no-additional] [--no-lfh-extra] [--no-offsets] [--no-ordering]
                        ZIPFILE1 ZIPFILE2
$ diff-zip-meta.py a.apk b.apk
--- a.apk
+++ b.apk
entry foo:
- compresslevel=6
+ compresslevel=9
- compress_crc=0x9ed711dc
+ compress_crc=0xd9776b0c
$ diff-zip-meta.py a.apk c.apk --no-offsets --no-ordering
--- a.apk
+++ c.apk
entries (sorted by filename):
- filename=META-INF/CERT.RSA
- filename=META-INF/CERT.SF
- filename=META-INF/MANIFEST.MF
central directory:
  data_before:
-   aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
-   bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
entry foo:
- compresslevel=6
+ compresslevel=9
- compress_crc=0x9ed711dc
+ compress_crc=0xd9776b0c
entry META-INF/com/android/build/gradle/app-metadata.properties:
  data_before (entry):
-   504b030400000000000021082102000000000000000000000000000066000000000000000000
-   0000000000000000000000000000000000000000000000000000000000000000000000000000
-   0000000000000000000000000000000000000000000000000000000000000000000000000000
-   000000000000000000000000000000000000
```

NB: work in progress; output format may change.

### dump-arsc.py

Dump `resources.arsc` (extracted or inside an APK) using `aapt2`.

```bash
$ dump-arsc.py --help
usage: dump-arsc.py [-h] [--apk] ARSC_OR_APK
[...]
$ dump-arsc.py resources.arsc
Binary APK
Package name=com.example.app id=7f
[...]
$ dump-arsc.py --apk some.apk
Binary APK
Package name=com.example.app id=7f
[...]
```

### dump-axml.py

Dump Android binary XML (extracted or inside an APK) using `aapt2`.

```bash
$ dump-axml.py --help
usage: dump-axml.py [-h] [--apk APK] AXML
[...]
$ dump-axml.py foo.xml
N: android=http://schemas.android.com/apk/res/android (line=17)
  E: selector (line=17)
      E: item (line=18)
[...]
$ dump-axml.py --apk some.apk res/foo.xml
N: android=http://schemas.android.com/apk/res/android (line=17)
  E: selector (line=17)
      E: item (line=18)
[...]
```

### dump-baseline.py

Dump `baseline.prof`/`baseline.profm` (extracted or inside an APK).

```bash
$ dump-baseline.py --help
usage: dump-baseline.py [-h] [--apk] [-v] PROF_OR_APK
[...]
$ dump-baseline.py baseline.prof
prof version=010 P
num_dex_files=4
[...]
$ dump-baseline.py baseline.profm
profm version=002
num_dex_files=4
[...]
$ dump-baseline.py some.apk
entry=assets/dexopt/baseline.prof
prof version=010 P
num_dex_files=4
[...]
entry=assets/dexopt/baseline.profm
profm version=002
num_dex_files=4
[...]
```

NB: does not support all file format versions yet.

### list-compresslevel.py

List ZIP entries with compression level.

You can optionally specify which files to list by providing one or more
fnmatch-style patterns, e.g. `'assets/foo/*.bar'`.

```bash
$ list-compresslevel.py --help
usage: list-compresslevel.py [-h] APK [PATTERN ...]
[...]
$ list-compresslevel.py some.apk
filename='AndroidManifest.xml' compresslevel=9|6
filename='classes.dex' compresslevel=None
filename='resources.arsc' compresslevel=None
[...]
filename='META-INF/CERT.SF' compresslevel=9|6
filename='META-INF/CERT.RSA' compresslevel=9|6|4
filename='META-INF/MANIFEST.MF' compresslevel=9|6|4
```

NB: the compression level is not actually stored anywhere in the ZIP file, and
is thus calculated by recompressing the data with different compression levels
and checking the CRC32 of the result against the CRC32 of the original
compressed data.

### zipinfo.py

List ZIP entries (like `zipinfo`).

This implementation aims for compatibility with the default and `-l` output
formats of Info-ZIP's `zipinfo`; the `-e` extended output format is unique to
this implementation.  Other formats and options are (currently) not supported.

Neither is the full variety of ZIP formats and extensions supported, just the
most common ones (UNIX, FAT, NTFS).

The `-l`/`--long` option adds the compressed size before the compression type;
`-e`/`--extended` does the same, adds the CRC32 checksum before the filename as
well, uses a more standard date format, and treats filenames ending with a `/`
as directories.

```bash
$ zipinfo.py --help
usage: zipinfo.py [-h] [-1] [-e] [-l] [--sort-by-offset] ZIPFILE
[...]
$ zipinfo.py -e some.apk
Archive:  some.apk
Zip file size: 5612 bytes, number of entries: 8
drw----     2.0 fat        0 bX        2 defN 2017-05-15 11:25:18 00000000 META-INF/
-rw----     2.0 fat       77 bl       76 defN 2017-05-15 11:25:18 b506b894 META-INF/MANIFEST.MF
-rw----     2.0 fat     1672 bl      630 defN 2009-01-01 00:00:00 615ef200 AndroidManifest.xml
-rw----     1.0 fat     1536 b-     1536 stor 2009-01-01 00:00:00 9987d5d8 classes.dex
-rw----     2.0 fat       29 bl        6 defN 2017-05-15 11:26:52 ff801cd1 temp.txt
-rw----     1.0 fat        6 b-        6 stor 2017-05-15 11:24:32 31963516 lib/armeabi/fake.so
-rw----     1.0 fat      896 b-      896 stor 2009-01-01 00:00:00 4fcab821 resources.arsc
-rw----     2.0 fat       20 bl        6 defN 2017-05-15 11:28:40 c9983e85 temp2.txt
8 files, 4236 bytes uncompressed, 3158 bytes compressed:  25.4%
$ zipinfo.py -l some.apk
Archive:  some.apk
Zip file size: 5612 bytes, number of entries: 8
-rw----     2.0 fat        0 bX        2 defN 17-May-15 11:25 META-INF/
-rw----     2.0 fat       77 bl       76 defN 17-May-15 11:25 META-INF/MANIFEST.MF
-rw----     2.0 fat     1672 bl      630 defN 09-Jan-01 00:00 AndroidManifest.xml
-rw----     1.0 fat     1536 b-     1536 stor 09-Jan-01 00:00 classes.dex
-rw----     2.0 fat       29 bl        6 defN 17-May-15 11:26 temp.txt
-rw----     1.0 fat        6 b-        6 stor 17-May-15 11:24 lib/armeabi/fake.so
-rw----     1.0 fat      896 b-      896 stor 09-Jan-01 00:00 resources.arsc
-rw----     2.0 fat       20 bl        6 defN 17-May-15 11:28 temp2.txt
8 files, 4236 bytes uncompressed, 3158 bytes compressed:  25.4%
```

The fields are: permissions, create version, create system, uncompressed size,
extra info, compressed size (w/ `--long` or `--extended`), compression type,
date, time, CRC32 (w/ `--extended`), filename.

The extra info field consists of two characters: the first is `b` for binary,
`t` for text (uppercase for encrypted files); the second is `X` for data
descriptor and extra field, `l` for just data descriptor, `x` for just extra
field, `-` for neither.

See also:
[`zipinfo(1)`](https://manpages.debian.org/stable/unzip/zipinfo.1.en.html),
[`zipdetails(1)`](https://manpages.debian.org/stable/perl/zipdetails.1.en.html).

## helper scripts

### inplace-fix.py

Convenience wrapper for some of the other scripts like `fix-newlines` that makes
them modify the file in-place (and optionally `zipalign` it too).

```bash
$ inplace-fix.py --help
usage: inplace-fix.py [-h] [--zipalign] [--page-align] COMMAND INPUT_FILE [...]
[...]
$ inplace-fix.py --zipalign fix-newlines unsigned.apk 'META-INF/services/*'
[RUN] python3 fix-newlines.py unsigned.apk /tmp/.../fixed.apk META-INF/services/*
fixing 'META-INF/services/foo'...
fixing 'META-INF/services/bar'...
[RUN] zipalign 4 /tmp/.../fixed.apk /tmp/.../aligned.apk
[MOVE] /tmp/.../aligned.apk to unsigned.apk
```

If `zipalign` is not found on `$PATH` but any of `$ANDROID_HOME`,
`$ANDROID_SDK`, or `$ANDROID_SDK_ROOT` is set to an Android SDK directory, it
will use `zipalign` from the latest `build-tools` subdirectory of the Android
SDK.

NB: however, it will skip `build-tools` `31.0.0` and `32.0.0` because
[their `zipalign` is broken](https://android.googlesource.com/platform/build/+/df73d1b4733b8b3cdfd96199018455026ba8d9d2).

NB: this script is not available as a `repro-apk` subcommand, but as a seperate
`repro-apk-inplace-fix` command.

## gradle integration

You can e.g. sort `baseline.profm` during the `gradle` build by adding something
like this to your `build.gradle`:

<details>

```gradle
// NB: assumes reproducible-apk-tools is a submodule in the app repo's
// root dir; adjust the path accordingly if it is found elsewhere
project.afterEvaluate {
    tasks.compileReleaseArtProfile.doLast {
        outputs.files.each { file ->
            if (file.name.endsWith(".profm")) {
                exec {
                    commandLine(
                        "../reproducible-apk-tools/inplace-fix.py",
                        "sort-baseline", file
                    )
                }
            }
        }
    }
}
```

</details>

Alternatively, adding something like this allows you to modify the APK itself
after building (and re-sign it if necessary):

<details>

```gradle
// NB: assumes reproducible-apk-tools is a submodule in the app repo's
// root dir; adjust the path accordingly if it is found elsewhere
android {
    applicationVariants.all { variant ->
        variant.outputs.each { output ->
            variant.packageApplicationProvider.get().doLast {
                exec {
                    // set ANDROID_HOME for zipalign
                    environment "ANDROID_HOME", android.sdkDirectory
                    commandLine(
                        "../reproducible-apk-tools/inplace-fix.py",
                        "--zipalign", "fix-newlines", output.outputFile,
                        "META-INF/services/*"
                    )
                }
                // re-sign w/ apksigner if needed
                if (variant.signingConfig != null) {
                    def tools = "${android.sdkDirectory}/build-tools/${android.buildToolsVersion}"
                    def sc = variant.signingConfig
                    exec {
                        environment "KS_PASS", sc.storePassword
                        environment "KEY_PASS", sc.keyPassword
                        commandLine(
                            "${tools}/apksigner", "sign", "-v",
                            "--ks", sc.storeFile,
                            "--ks-pass", "env:KS_PASS",
                            "--ks-key-alias", sc.keyAlias,
                            "--key-pass", "env:KEY_PASS",
                            output.outputFile
                        )
                    }
                }
            }
        }
    }
}
```

</details>

## fnmatch-style patterns

Some of these scripts process/list files matching any of the provided patterns
using Python's `fnmatch.fnmatch()`, Unix shell style:

```
*       matches everything
?       matches any single character
[seq]   matches any character in seq
[!seq]  matches any char not in seq
```

With one addition: an optional prefix `!` negates the pattern, invalidating a
successful match by any preceding pattern; use a backslash (`\`) in front of the
first `!` for patterns that begin with a literal `!`.

NB: to match e.g. everything except for `*.xml`, you need to provide two
patterns: the first (`'*'`) to match everything, the second (`'!*.xml'`) to
negate matching `*.xml`.

NB: `*` matches anything, including `/`, and the pattern matches the complete
filename path, including leading directories, so e.g. `foo/bar.baz` is matched
by both `*.baz` and `foo/*`.

## CLI

NB: you can just use the scripts stand-alone; alternatively, you can install the
`repro-apk` Python package and use them as subcommands of `repro-apk`:

```bash
$ repro-apk diff-zip-meta a.apk b.apk
$ repro-apk diff-zip-meta a.apk c.apk --no-offsets --no-ordering
$ repro-apk dump-arsc resources.arsc
$ repro-apk dump-arsc --apk some.apk
$ repro-apk dump-axml foo.xml
$ repro-apk dump-axml --apk some.apk res/foo.xml
$ repro-apk dump-baseline baseline.prof
$ repro-apk dump-baseline baseline.profm
$ repro-apk dump-baseline --apk some.apk
$ repro-apk fix-compresslevel unsigned.apk fixed.apk 6 assets/foo/bar.js
$ repro-apk fix-files unsigned.apk fixed.apk unix2dos 'META-INF/services/*'
$ repro-apk fix-newlines unsigned.apk fixed.apk 'META-INF/services/*'
$ repro-apk list-compresslevel some.apk
$ repro-apk rm-files some.apk fixed.apk META-INF/MANIFEST.IN
$ repro-apk sort-apk unsigned.apk sorted.apk
$ repro-apk sort-baseline baseline.profm baseline-sorted.profm
$ repro-apk sort-baseline --apk unsigned.apk sorted-baseline.apk
$ repro-apk zipalign fixed.apk fixed-aligned-py.apk
$ repro-apk zipinfo -e some.apk
$ repro-apk zipinfo -l some.apk
```

### Help

```bash
$ repro-apk --help
$ repro-apk diff-zip-meta --help
$ repro-apk dump-arsc --help
$ repro-apk dump-axml --help
$ repro-apk dump-baseline --help
$ repro-apk fix-compresslevel --help
$ repro-apk fix-files --help
$ repro-apk fix-newlines --help
$ repro-apk list-compresslevel --help
$ repro-apk rm-files --help
$ repro-apk sort-apk --help
$ repro-apk sort-baseline --help
$ repro-apk zipalign --help
$ repro-apk zipinfo --help
```

## Installing

### Using pip

```bash
$ pip install repro-apk
```

NB: depending on your system you may need to use e.g. `pip3 --user`
instead of just `pip`.

### From git

NB: this installs the latest development version, not the latest
release.

```bash
$ git clone https://github.com/obfusk/reproducible-apk-tools.git
$ cd reproducible-apk-tools
$ pip install -e .
```

NB: you may need to add e.g. `~/.local/bin` to your `$PATH` in order
to run `repro-apk`.

To update to the latest development version:

```bash
$ cd reproducible-apk-tools
$ git pull --rebase
```

## Dependencies

* Python >= 3.8 + click (`repro-apk` package only, the stand-alone scripts have
  no dependencies besides Python).

* The `dump-arsc.py` and `dump-axml.py` scripts require `aapt2`.

### Debian/Ubuntu

```bash
$ apt install python3-click
$ apt install aapt      # for dump-arsc.py & dump-axml.py
$ apt install zipalign  # for realignment; see examples
```

## License

[![GPLv3+](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.html)

<!-- vim: set tw=70 sw=2 sts=2 et fdm=marker : -->
