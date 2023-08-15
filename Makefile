CONTAINER_NAME=deku_sms_container_$(apk)
CONTAINER_NAME_1=deku_sms_container_$(apk)_1

APP_1=$(apk).apk
APP_2=$(apk)_1.apk

pass=$$(cat ks.passwd)
branch_name=$$(git symbolic-ref HEAD)
version=$$(cat version.properties | cut -d "=" -f 2)

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
	@if [ "$(branch_name)" == "refs/heads/master" ] || [ "$(branch_name)" == "refs/heads/staging" ]; then \
		python3 bump_version.py && \
		git add . && \
		git commit -m "release: making release"; \
	else \
		echo "[Error] wrong branch - $(branch_name)"; \
		exit 1; \
	fi

release-local: bump_version
	@echo "Building apk output: ${version}"
	@./gradlew clean assembleRelease
	@apksigner sign --ks app/keys/app-release-key.jks \
		--ks-pass pass:$(pass) \
		--in app/build/outputs/apk/release/app-release-unsigned.apk \
		--out apk-outputs/${version}.apk
	@shasum apk-outputs/${version}.apk
	@./gradlew clean bundleRelease
	@apksigner sign --ks app/keys/app-release-key.jks \
		--ks-pass pass:$(pass) \
		--in app/build/outputs/bundle/release/app-release-unsigned.aab \
		--out apk-outputs/${version}.aab
	@shasum apk-outputs/${version}.aab
