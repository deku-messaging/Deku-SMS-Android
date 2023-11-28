SHELL   := /bin/bash
PYTHON  ?= python3

export PYTHONWARNINGS := default

.PHONY: all install test test-cli doctest test-examples lint lint-extra clean cleanup

all:

install:
	$(PYTHON) -mpip install -e .

test: test-cli doctest lint lint-extra

test-cli:
	# TODO
	repro-apk --version

doctest:
	# NB: uses test/ & requires zipalign on $PATH
	TZ=UTC $(PYTHON) -m doctest fix-compresslevel.py fix-files.py fix-newlines.py \
	  inplace-fix.py list-compresslevel.py rm-files.py zipinfo.py

test-examples:
	mkdir -p .tmp
	# fix-compresslevel
	repro-apk fix-compresslevel test/data/level6.apk .tmp/level6-to-9.apk 9 'LICENSE.*'
	zipalign -f 4 .tmp/level6-to-9.apk .tmp/level6-to-9-aligned.apk
	cmp test/data/level9.apk .tmp/level6-to-9-aligned.apk
	repro-apk fix-compresslevel test/data/level9.apk .tmp/level9-to-6.apk 6 'LICENSE.*'
	zipalign -f 4 .tmp/level9-to-6.apk .tmp/level9-to-6-aligned.apk
	cmp test/data/level6.apk .tmp/level9-to-6-aligned.apk
	# fix-files
	repro-apk fix-files test/data/unix.apk .tmp/unix2dos.apk unix2dos 'LICENSE.*'
	zipalign -f 4 .tmp/unix2dos.apk .tmp/unix2dos-aligned.apk
	cmp test/data/crlf.apk .tmp/unix2dos-aligned.apk
	# fix-newlines
	repro-apk fix-newlines test/data/unix.apk .tmp/unix-to-crlf.apk 'LICENSE.*'
	zipalign -f 4 .tmp/unix-to-crlf.apk .tmp/unix-to-crlf-aligned.apk
	cmp test/data/crlf.apk .tmp/unix-to-crlf-aligned.apk
	repro-apk fix-newlines --from-crlf test/data/crlf.apk .tmp/crlf-to-unix.apk 'LICENSE.*'
	zipalign -f 4 .tmp/crlf-to-unix.apk .tmp/crlf-to-unix-aligned.apk
	cmp test/data/unix.apk .tmp/crlf-to-unix-aligned.apk
	# fix-newlines via repro-apk-inplace-fix
	cp test/data/unix.apk .tmp/unix-to-crlf-inplace.apk
	repro-apk-inplace-fix --zipalign fix-newlines .tmp/unix-to-crlf-inplace.apk 'LICENSE.*'
	cmp test/data/crlf.apk .tmp/unix-to-crlf-inplace.apk
	cp test/data/unix.apk .tmp/unix-to-crlf-inplace-p.apk
	repro-apk-inplace-fix --page-align fix-newlines .tmp/unix-to-crlf-inplace-p.apk 'LICENSE.*'
	cmp test/data/crlf-p.apk .tmp/unix-to-crlf-inplace-p.apk
	# rm-files (via repro-apk-inplace-fix as well)
	repro-apk rm-files test/data/baseline1.profm.apk .tmp/rm1.apk '*.profm'
	cp test/data/baseline2.profm.apk .tmp/rm2.apk
	repro-apk-inplace-fix rm-files .tmp/rm2.apk '*.profm'
	cmp .tmp/rm1.apk .tmp/rm2.apk
	# sort-apk
	repro-apk sort-apk test/data/golden-aligned-in.apk .tmp/sorted.apk
	cmp test/data/golden-aligned-in-sorted.apk .tmp/sorted.apk
	repro-apk sort-apk --no-realign --reset-lh-extra test/data/golden-aligned-in.apk \
	  .tmp/sorted-noalign.apk
	cmp test/data/golden-aligned-in-sorted-noalign.apk .tmp/sorted-noalign.apk
	# sort-baseline
	repro-apk sort-baseline test/data/baseline1.profm .tmp/baseline1-sorted.profm
	cmp test/data/baseline2.profm .tmp/baseline1-sorted.profm
	# sort-baseline via repro-apk-inplace-fix
	cp test/data/baseline1.profm .tmp/baseline1-inplace.profm
	repro-apk-inplace-fix sort-baseline .tmp/baseline1-inplace.profm
	cmp test/data/baseline2.profm .tmp/baseline1-inplace.profm
	# sort-baseline --apk
	repro-apk sort-baseline --apk test/data/baseline1.profm.apk \
	  .tmp/baseline1.profm-sorted.apk
	zipalign -f 4 .tmp/baseline1.profm-sorted.apk .tmp/baseline1.profm-sorted-aligned.apk
	cmp test/data/baseline2.profm.apk .tmp/baseline1.profm-sorted-aligned.apk
	# sort-baseline --apk via repro-apk-inplace-fix
	cp test/data/baseline1.profm.apk .tmp/baseline1.profm-inplace.apk
	repro-apk-inplace-fix --zipalign sort-baseline --apk .tmp/baseline1.profm-inplace.apk
	cmp test/data/baseline2.profm.apk .tmp/baseline1.profm-inplace.apk
	# diff-zip-meta
	cd test/data && diff -Naur golden-aligned-in-sorted.diff \
	  <( repro-apk diff-zip-meta golden-aligned-in.apk golden-aligned-in-sorted.apk )
	cd test/data && diff -Naur golden-aligned-in-sorted-no-lfh-ord-off.diff \
	  <( repro-apk diff-zip-meta golden-aligned-in.apk golden-aligned-in-sorted.apk \
	     --no-lfh-extra --no-offsets --no-ordering )
	cd test/data && diff -Naur golden-aligned-in-sorted-noalign.diff \
	  <( repro-apk diff-zip-meta golden-aligned-in-sorted.apk \
	     golden-aligned-in-sorted-noalign.apk )
	cd test/data && diff -Naur level6-9.diff \
	  <( repro-apk diff-zip-meta level6.apk level9.apk )
	cd test/data && diff -Naur unix-crlf.diff \
	  <( repro-apk diff-zip-meta unix.apk crlf.apk )
	cd test/data && diff -Naur unix-6-no-off.diff \
	  <( repro-apk diff-zip-meta unix.apk level6.apk --no-offsets )
	# dump-arsc
	cd test/data && diff -Naur resources1.arsc.dump \
	  <( repro-apk dump-arsc resources1.arsc )
	cd test/data && diff -Naur resources2.arsc.dump \
	  <( repro-apk dump-arsc resources2.arsc )
	# dump-arsc --apk
	cd test/data && diff -Naur golden-aligned-in-arsc.dump \
	  <( repro-apk dump-arsc --apk crlf.apk )
	# dump-axml
	cd test/data && diff -Naur AndroidManifest.xml.dump \
	  <( repro-apk dump-axml AndroidManifest.xml )
	cd test/data && diff -Naur main.xml.dump \
	  <( repro-apk dump-axml main.xml )
	# dump-axml --apk
	cd test/data && diff -Naur golden-aligned-in-axml.dump \
	  <( repro-apk dump-axml --apk golden-aligned-in.apk AndroidManifest.xml )
	# dump-baseline .prof
	cd test/data && diff -Naur <( gunzip < baseline1.prof.dump.gz ) \
	  <( repro-apk dump-baseline -v baseline1.prof )
	cd test/data && diff -Naur <( gunzip < baseline2.prof.dump.gz ) \
	  <( repro-apk dump-baseline -v baseline2.prof )
	# dump-baseline .profm
	cd test/data && diff -Naur baseline1.profm.dump \
	  <( repro-apk dump-baseline -v baseline1.profm )
	cd test/data && diff -Naur baseline2.profm.dump \
	  <( repro-apk dump-baseline -v baseline2.profm )
	# dump-baseline --apk .prof
	# TODO
	# dump-baseline --apk .profm
	cd test/data && diff -Naur \
	  <( echo entry=assets/dexopt/baseline.profm; cat baseline1.profm.dump ) \
	  <( repro-apk dump-baseline --apk -v baseline1.profm.apk )
	cd test/data && diff -Naur \
	  <( echo entry=assets/dexopt/baseline.profm; cat baseline2.profm.dump ) \
	  <( repro-apk dump-baseline --apk -v baseline2.profm.apk )
	# list-compresslevel
	cd test/data && diff -Naur level6.levels \
	  <( repro-apk list-compresslevel level6.apk )
	cd test/data && diff -Naur level9.levels \
	  <( repro-apk list-compresslevel level9.apk )
	cd test/data && diff -Naur <( echo "filename='LICENSE.GPLv3' compresslevel=6" ) \
	  <( repro-apk list-compresslevel unix.apk LICENSE.GPLv3 )
	# zipalign
	repro-apk zipalign .tmp/level6-to-9.apk .tmp/level6-to-9-aligned-py.apk
	cmp .tmp/level6-to-9-aligned.apk .tmp/level6-to-9-aligned-py.apk
	repro-apk zipalign .tmp/level9-to-6.apk .tmp/level9-to-6-aligned-py.apk
	cmp .tmp/level9-to-6-aligned.apk .tmp/level9-to-6-aligned-py.apk
	repro-apk zipalign .tmp/unix2dos.apk .tmp/unix2dos-aligned-py.apk
	cmp .tmp/unix2dos-aligned.apk .tmp/unix2dos-aligned-py.apk
	repro-apk zipalign .tmp/unix-to-crlf.apk .tmp/unix-to-crlf-aligned-py.apk
	cmp .tmp/unix-to-crlf-aligned.apk .tmp/unix-to-crlf-aligned-py.apk
	repro-apk zipalign .tmp/crlf-to-unix.apk .tmp/crlf-to-unix-aligned-py.apk
	cmp .tmp/crlf-to-unix-aligned.apk .tmp/crlf-to-unix-aligned-py.apk
	repro-apk zipalign .tmp/baseline1.profm-sorted.apk .tmp/baseline1.profm-sorted-aligned-py.apk
	cmp .tmp/baseline1.profm-sorted-aligned.apk .tmp/baseline1.profm-sorted-aligned-py.apk
	set -e; for apk in test/data/*.apk; do echo "$$apk"; \
	  zipalign -f 4 "$$apk" .tmp/aligned.apk; \
	  repro-apk zipalign "$$apk" .tmp/aligned-py.apk; \
	  cmp .tmp/aligned.apk .tmp/aligned-py.apk; \
	done
	# zipinfo
	set -e; cd test/data && for apk in *.apk; do echo "$$apk"; \
	  diff -Naur <( zipinfo    "$$apk" ) <( repro-apk zipinfo    "$$apk" ); \
	  diff -Naur <( zipinfo -l "$$apk" ) <( repro-apk zipinfo -l "$$apk" ); \
	done

lint:
	set -x; flake8 repro_apk/*.py
	set -x; pylint repro_apk/*.py

lint-extra:
	set -x; mypy --strict --disallow-any-unimported repro_apk/*.py

clean: cleanup
	rm -fr repro_apk.egg-info/

cleanup:
	find -name '*~' -delete -print
	rm -fr repro_apk/__pycache__/ .mypy_cache/
	rm -fr build/ dist/
	rm -fr .coverage htmlcov/
	rm -fr .tmp/

.PHONY: _package _publish

_package:
	SOURCE_DATE_EPOCH="$$( git log -1 --pretty=%ct )" \
	  $(PYTHON) setup.py sdist bdist_wheel
	twine check dist/*

_publish: cleanup _package
	read -r -p "Are you sure? "; \
	[[ "$$REPLY" == [Yy]* ]] && twine upload dist/*
