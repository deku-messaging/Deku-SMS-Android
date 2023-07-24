debug:
	docker build -t deku_sms_app . && \
		docker run --name deku_sms_container deku_sms_app && \
		docker cp deku_sms_container:/android/app/build/outputs/apk/debug/app-debug.apk builds/ && \
		docker rm deku_sms_container
