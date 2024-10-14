# DekuSMS
--------
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.afkanerd.deku/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.afkanerd.deku)

     
<b>Contents</b>

[About](#about)

[Build](#build)

[Reproducible builds](#reproducible_builds)

# <a name="about"></a> About
DekuSMS is an Android SMS app. 

DekuSMS is being developed as a proof-of-concept for secure SMS messaging, SMS image transmission and SMS-Cloud communications. 

The technical functionalities of the app are currently not user friendly, which would be updated with the help of PRs and issues. The reason for the lack of user friendliness is solely based on the app aiming to be as customizable as possible. Users should be able to configure the app to their custom cloud servers without being tied into using specific providers.

Join us on [Telegram](https://t.me/deku_sms) for feature release announcements or general SMS and security discussions.

Credits to the new logo goes to: [Erenye4g3r](https://github.com/Erenye4g3r)

<b>Features</b>

[End-to-End encryption](#e2e_encryption)

[Ability to forward incoming messages to cloud server](#cloud_forward)

[Ability to use mobile phone as an SMS Gateway to send messages from the cloud](#sms_gateway)

## <a name="e2e_encryption"></a> End to End Encryption
DekuSMS supports end to end encryption (with Perfect Forward Secrecry - PFS). For the feature to work, both users need to be using DekuSMS as their default SMS app; This is because DekuSMS uses customed data structures for perfoming handshakes and encryptions. This methods are not built into other SMS apps and for those who can support it, they will most likely build a custom data structure for themselves. Willing to discuss with other SMS apps and developers for a unified FOSS protocol for e2ee SMS messaging - starting with Android.

Do not worry if you send an request to someone who does not use the App, there is a great chance they would not see it; DekuSMS uses data channels (does not require an internet connection) for sending secure requests and most SMS apps would dump messages coming in from that channel (especially on iPhones). If the user does not have DekuSMS or an SMS app made by someone weird enough to encode byte data from data channels they will not see the request.

The users are guided through a Diffie-Hellman key exchange handshake, after which a secure key is generated and associated with the peers phone number; 32 Bytes agreement keys are generated for this - ~50 Bytes to be transmitted by SMS and let's call this the <b>SK</b>. The <b>SK</b> becomes the <b>RK</b> for the first messages being sent - and thus begins the era of the <b>KeyChains</b>. You have got to read this, to see what we are working with here [https://signal.org/docs/specifications/doubleratchet/](https://signal.org/docs/specifications/doubleratchet/).

Every message being sent has a <b>Public Key</b> attached to it and some other details (call this the <b>Headers</b>). This means the size of each message is atleast 50 Bytes for the Header followed by the contents. The headers are not encrypted (yet). 

To count in SMS, for every SMS being sent you have 1 additional SMS added as the header; if you compose an SMS of body size 1 (140 bytes) you send 1 + 1(header) = 2 messages. If you compose an SMS of body size 2 (140 * 2 bytes) you get 3 messages being sent (2 + 1(header)). SMS text (UTF-7/8) encoded messages can segment themselves, which means multiple messages can be sent and then joint on the receiving end to make one message; that's why extremely long SMS messages can be received - infact you can encode an entire image to Base64 and transmit via SMS thanks to this (has some issues though so be careful with sending that large a message).

Also good to remember, the size of your composed messages is not the same size as the encrypted message (but with better UI/UX persons helping on this project one should be able to know how large a message they might be sending already). 

The users cannot know when their peer has deleted their secured keys; This means messages will still be received by peer in encrypted form. Good to know however, because if you and I are texting with encrypted messages and I tell you stuff, you better have encryption turned on because no way I will tell more stuff in plain text. The eventual goal (eventual because have not programmed that yet) is, when you reply back to me or send me any message that is in plain text, I then know you have lost your encryption keys and continue communicating in plain text too.

All messages are currently being stored in the default SMS inbox (including encrypted messages). Users switching between SMS apps would still maintain their inbox as it is. Upcoming features would remove encrypted messages to custom database if users intends.

Note to developers: There is a race condition right after the first keys have been exchanged. Anyone can be Alice, but they'd be an issue of both are Alice because Service providers decide to not deliver the message from the true Alice on time; because SMS :(


## <a name="cloud_forward"></a> Forward incoming messages to cloud
Forward incoming SMS messages to custom url. The messages remain in queue till the device has an active connection. The messages remain queue if server status codes is in range `5xx`. The device also provides visibility into the messages being forwarded into the cloud.

The content being forwarded to your cloud look like:

```json
 {
"id": "long()",
"message_id":"str",
"thread_id":"str",
"date":"str",
"date_sent":"str",
"type":"int()",
"num_segments":"int()",
"subscription_id":"int()",
"status":"int()",
"error_code":"int()",
"read":"int()",
"is_encrypted":"int()",
"formatted_date":"str",
"address":"str",
"text":"str",
"data":"str"
}
```

<b>Supported Protocols</b>
- HTTP(s)
- (S)FTP
- SMTP

## <a name="sms_gateway"></a> Android phone as SMS Gateway
You can transmit data from your to your cloud to Android devices, using your Android phones as your SMS Gateway.

The app has direct built configurations for [RabbitMQ](#https://www.rabbitmq.com/) allowing technical users
to configure their own messaging queue directly from their own server.

The structure of the incoming RabbitMQ messages is a json like:
```json
{"sid":"", "id":"", "to":"", "text":""}
```
> <b>sid:</b> special id you can include in the message going to your messaging queue server
> 
> <b>id:</b> uniquely generated by messaging queue server - should not be used in determining the status of messages. Primarily used to inform messaging queue server to `ack` the message.
> 
> <b>to:</b> phone number of the receipient. Should be in [E.164 Format](https://support.twilio.com/hc/en-us/articles/223183008-Formatting-International-Phone-Numbers).
> 
> <b>text:</b> the message to be sent to the receipient.

The app sends a callback to your SMS Gateways once the requested message [status changes]()*.
```json
{"type":"SMS_TYPE_STATUS", "status":"[status_changes]()", "sid":""}
```

<b>* Status changes</b> - `sent`, `delivered`, `failed`

# <a name="build"></a> Build
```bash
git clone https://github.com/deku-messaging/Deku-SMS-Android.git
git submodule update --init --recursive
```
Getting the project into Android-studio would allow for an easy build.

## <a name="reproducible_builds"></a> Reproducible builds notes
Check a build against any previous commits. Requires docker
- `commit` = hash of commit being compared
- `commit_url` = url of the repo from git (should have been called repo_url)
- `release_url` = url of the github release to build against (or any place where the apk can be downloaded)
- `jks` = path to signing key file
- `jks_pass` = password for jks

```bash
make check-commits \
commit= \
commit_url= \
release_url= \
jks= \
jks_pass=
```

- https://f-droid.org/docs/Reproducible_Builds/
