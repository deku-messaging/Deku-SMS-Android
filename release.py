#!/usr/bin/python3
import re
import requests
import json
import sys, os
import logging
import httplib2


class RelGooglePlaystore:
    def create_edit_for_draft_release(self, 
                                      version_code, 
                                      version_name, 
                                      description, 
                                      bundle_file,
                                      status='draft',
                                      track='internal', 
                                      timeout_seconds=600, 
                                      changesNotSentForReview = True):
        """
        """
        from googleapiclient.discovery import build
        from google.oauth2 import service_account 
        from googleapiclient.http import MediaFileUpload  # Import MediaFileUpload

        credentials_file_path = None
        package_name = None

        with open('release.properties', 'r') as fd:
            lines = fd.readlines()

        for line in lines:
            if line.startswith('google_playstore_creds_filepath'):
                credentials_file_path = line.split("=")[1].strip() 
            elif line.startswith('app_package_name'):
                package_name = line.split("=")[1].strip() 
        # Create an HTTP object with a timeout

        http = httplib2.Http(timeout=timeout_seconds)

        credentials = service_account.Credentials.from_service_account_file(credentials_file_path)
        credentials.http = http

        service = build(serviceName='androidpublisher', version='v3', 
                        credentials=credentials,
                        num_retries=5)

        # return service.edits().insert(editId=release_id, body=edit_body).execute()
        edit_request = service.edits().insert(packageName=package_name)
        edit_response = edit_request.execute()
        edit_id = edit_response['id']


        # Create a media upload request
        media_upload = MediaFileUpload(bundle_file, 
                                       mimetype="application/octet-stream", resumable=True)

        bundle_response = service.edits().bundles().upload(
                packageName=package_name, 
                editId=edit_id, 
                media_body=media_upload
            ).execute()

        bundle_version_code = bundle_response['versionCode']

        # Specify the version code for the draft release
        version_code = bundle_version_code  # Use the version code of the uploaded bundle

        # version_code = 26
        release_body = [{
                'name':version_name,
                'status':status,
                'versionCodes':[version_code]
                }]

        track_request = service.edits().tracks().update(
                packageName=package_name,
                editId=edit_id,
                track=track,
                body={'releases': release_body}
                )

        response = track_request.execute()
        logging.info("[Playstore] %s release %s created with version code %d", status, version_name, version_code)

        # Commit the changes to finalize the edit
        commit_request = service.edits().commit(
            packageName=package_name,
            editId=edit_id
        )
        commit_request.execute()

        logging.info("[Playstore] Changes committed and edit finalized.")


class RelGithub:
    def create_edit_for_draft_release(self, 
                                      version_code, 
                                      version_name, 
                                      description, 
                                      target_branch, 
                                      status, 
                                      url,
                                      apk_file):
        # Create a new release on GitHub.

        status = True if status == 'draft' else False

        # url = "https://api.github.com/repos/deku-messaging/Deku-SMS-Android/releases"
        data = {
            "tag_name": str(version_code),
            "name": version_name,
            "body": description,
            "target_commitish": target_branch,
            "draft": status,
            "prerelease":False,
            "generate_release_notes":False
        }
        logging.info(data)

        github_token = None

        with open('release.properties', 'r') as fd:
            lines = fd.readlines()

        for line in lines:
            if line.startswith('github_token'):
                github_token = line.split("=")[1].strip() 
                break

        headers = {"Authorization": "Bearer {}".format(github_token), 
                   "X-GitHub-Api-Version": "2022-11-28",
                   "Accept": "application/vnd.github+json"}

        response = requests.post(url, json=data, headers=headers)
        response.raise_for_status()

        logging.info("[GitHub] Create new release: %d", response.status_code)
        response = json.loads(response.text)
        upload_url = response['upload_url']

        # Upload assets to a new release on GitHub.
        headers = {'Content-Type': 'application/octet-stream', 
                   "Authorization": "Bearer {}".format(github_token), 
                   "X-GitHub-Api-Version": "2022-11-28",
                   "Accept": "application/vnd.github+json"}

        upload_url = re.sub(r"\{\?name,label}", "", upload_url)

        params = {
            'name': os.path.basename(apk_file),
            'label': version_name
        }

        with open(apk_file, 'rb') as f:
            data = f.read()

        response = requests.post(upload_url, headers=headers, data=data, params=params)
        response.raise_for_status()

        logging.info("[GitHub] Create upload release: %d", response.status_code)
        # return json.loads(response.text)


if __name__ == "__main__":
    import argparse
    import threading

    parser = argparse.ArgumentParser(description="An argument parser for Python")

    parser.add_argument("--version_code", type=int, required=True, help="The version code of the app")
    parser.add_argument("--version_name", type=str, required=True, help="The version name of the app")
    parser.add_argument("--description", type=str, required=True, help="The description of the app")
    parser.add_argument("--branch", type=str, required=True, help="The branch of the app")
    parser.add_argument("--track", type=str, required=True, help="The track of the app")
    parser.add_argument("--app_bundle_file", type=str, required=True, help="The app bundle file")
    parser.add_argument("--app_apk_file", type=str, required=True, help="The app APK file")
    parser.add_argument("--status", type=str, required=True, help="The app release status")
    parser.add_argument("--github_url", type=str, required=True, help="The github repo URL")
    parser.add_argument("--log_level", type=str, default='INFO', required=False, help="The level of the log")
    parser.add_argument("--platform", type=str, default="all", required=False, help="Platform to be released on: \
            playstore, github")

    args = parser.parse_args()

    rel_playstore = RelGooglePlaystore()
    thread_playstore = threading.Thread(target=rel_playstore.create_edit_for_draft_release, args=(
        args.version_code, args.version_name, args.description, args.app_bundle_file, args.status, args.track, True,))

    rel_github = RelGithub()
    thread_github = threading.Thread(target=rel_github.create_edit_for_draft_release, args=(
        args.version_code, args.version_name, args.description, args.branch, 
        args.status, args.github_url, args.app_apk_file,))

    logging.basicConfig(level=args.log_level)

    if args.platform == "all":
        thread_playstore.start()
        thread_playstore.join()

        thread_github.start()
        thread_github.join()

    elif args.platform == "playstore":
        thread_playstore.start()
        thread_playstore.join()

    elif args.platform == "github":
        thread_github.start()
        thread_github.join()

