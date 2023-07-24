release:
	@echo "Building APK output: $(APK)"
	docker build -t deku_sms_app . && \
		docker run --name deku_sms_container deku_sms_app && \
		docker cp deku_sms_container:/android/app/build/outputs/apk/release/app-release.apk builds/$(APK) && \
		docker rm deku_sms_container
