CONTAINER_NAME=deku_sms_container_$(apk)
CONTAINER_NAME_1=deku_sms_container_$(apk)_1

APP_1=$(apk).apk
APP_2=$(apk)_1.apk

pass=$$(cat ks.passwd)
branch_name=$$(git symbolic-ref HEAD)

branch=$$(git symbolic-ref HEAD | cut -d "/" -f 3)

releaseVersion=$$(sed -n '1p' version.properties | cut -d "=" -f 2)
stagingVersion=$$(sed -n '2p' version.properties | cut -d "=" -f 2)
nightlyVersion=$$(sed -n '3p' version.properties | cut -d "=" -f 2)
tagVersion=$$(sed -n '4p' version.properties | cut -d "=" -f 2)

label=${releaseVersion}.${stagingVersion}.${nightlyVersion}

aab_output=${label}.aab
apk_output=${label}.apk

minSdk=24

VERSION_PROPERTIES_FILENAME = version.properties
SIGNING_KEY_FILENAME = app/keys/app-release-key.jks
RELEASE_PROPERTIES_FILENAME = release.properties
BUMP_VERSION_PYTHON_FILENAME = bump_version.py
RELEASE_VERSION_PYTHON_FILENAME = release.py

config:
	@mkdir apk-outputs

download:
	curl -OJL https://raw.githubusercontent.com/deku-messaging/Deku-SMS-Android/staging/bump_version.py
	curl -OJL https://raw.githubusercontent.com/deku-messaging/Deku-SMS-Android/staging/release.py
	curl -OJL https://raw.githubusercontent.com/deku-messaging/Deku-SMS-Android/staging/version.properties

check:
	@if [ ! -f ${VERSION_PROPERTIES_FILENAME} ]; then \
		echo "+ [NOT FOUND] ${VERSION_PROPERTIES_FILENAME}"; \
		echo ">> This file is required for tracking the versions for releases"; \
	fi
	@if [ ! -f ${SIGNING_KEY_FILENAME} ]; then \
		echo "+ [NOT FOUND] ${SIGNING_KEY_FILENAME}"; \
		echo ">> This file is required for signing the apks"; \
	fi
	@if [ ! -f ${RELEASE_PROPERTIES_FILENAME} ]; then \
		echo "+ [NOT FOUND] ${RELEASE_PROPERTIES_FILENAME}"; \
		echo ">> This file holds the tokens for the various releases"; \
	fi
	@if [ ! -f ${BUMP_VERSION_PYTHON_FILENAME} ]; then \
		echo "+ [NOT FOUND] ${BUMP_VERSION_PYTHON_FILENAME}"; \
		echo ">> This file increments the version of the build"; \
	fi
	@if [ ! -f ${RELEASE_VERSION_PYTHON_FILENAME} ]; then \
		echo "+ [NOT FOUND] ${RELEASE_VERSION_PYTHON_FILENAME}"; \
		echo ">> This file releases the build on the various distribution outlets"; \
	fi

release-docker:
	@echo "Building apk output: ${APP_1}"
	@docker build -t deku_sms_app .
	@docker run --name ${CONTAINER_NAME} -e PASS=$(pass) deku_sms_app && \
		docker cp ${CONTAINER_NAME}:/android/app/build/outputs/apk/release/app-release.apk apk-outputs/${APP_1} && \
		docker rm ${CONTAINER_NAME}
	@sleep 3
	@echo "Building apk output: ${APP_2}"
	@docker run --name ${CONTAINER_NAME_1} -e PASS=$(pass) deku_sms_app && \
		docker cp ${CONTAINER_NAME_1}:/android/app/build/outputs/apk/release/app-release.apk apk-outputs/${APP_2} && \
		docker rm ${CONTAINER_NAME_1}
	@diffoscope apk-outputs/${APP_1} apk-outputs/${APP_2} && \
		echo "Build is reproducible!" || echo "BUILD IS NOT REPRODUCIBLE!!"

bump_version: 
	@python3 bump_version.py $(branch_name)
	@git add .
	@git commit -m "release: making release"

build-apk:
	@echo "+ Building apk output: ${apk_output} - ${branch_name}"
	@./gradlew clean assembleRelease
	@apksigner sign --ks app/keys/app-release-key.jks \
		--ks-pass pass:$(pass) \
		--in app/build/outputs/apk/release/app-release-unsigned.apk \
		--out apk-outputs/${apk_output}
	@shasum apk-outputs/${apk_output}

build-aab:
	@echo "+ Building aab output: ${aab_output} - ${branch_name}"
	@./gradlew clean bundleRelease
	@apksigner sign --ks app/keys/app-release-key.jks \
		--ks-pass pass:$(pass) \
		--in app/build/outputs/bundle/release/app-release.aab \
		--out apk-outputs/${aab_output} \
		--min-sdk-version ${minSdk}
	@shasum apk-outputs/${aab_output}

release: bump_version build-apk release.properties
	@echo "+ Target branch for relase: ${branch}"
	@git tag ${tagVersion}
	@git push origin ${branch_name}
	@git push --tag
	@python3 release.py \
		${tagVersion} \
		${label} \
		"New release: ${label} - build No:${tagVersion}" \
		${branch} \
		${apk_output} \
		apk-outputs/${apk_output}
