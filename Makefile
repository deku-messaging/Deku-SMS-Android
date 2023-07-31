CONTAINER_NAME=deku_sms_container_$(apk)
CONTAINER_NAME_1=deku_sms_container_$(apk)_1

APP_1=$(apk).apk
APP_2=$(apk)_1.apk

pass=$$(cat ks.passwd)

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

release-local:
	@echo "Building apk output: ${APP_1}"
	@./gradlew clean assembleRelease
	@apksigner sign --ks app/keys/app-release-key.jks \
		--ks-pass pass:$(pass) \
		--in app/build/outputs/apk/release/app-release-unsigned.apk \
		--out app/build/outputs/apk/release/app-release.apk
