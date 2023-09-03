#!/usr/bin/python3
import re
import requests
import json
import sys

from googleapiclient.discovery import build
# from google.oauth2.credentials import Credentials 
from google.oauth2 import service_account 

class RelGooglePlaystore:

    def create_edit_for_draft_release(self, credentials_file_path, release_id, edit_body, bundle_file): 
        package_name = "com.afkanerd.deku"
        track = 'internal'
        release_name = '0.0.1'

        credentials = service_account.Credentials.from_service_account_file(credentials_file_path)
        service = build('androidpublisher', 'v3', credentials=credentials)

        # return service.edits().insert(editId=release_id, body=edit_body).execute()
        edit_request = service.edits().insert(packageName=package_name)
        edit_response = edit_request.execute()
        edit_id = edit_response['id']

        bundle_response = service.edits().bundles().upload(
                packageName=package_name, 
                editId=edit_id, 
                media_mime_type="application/octet-stream",
                media_body=bundle_file,).execute()

        bundle_version_code = bundle_response['versionCode']

        # Specify the version code for the draft release
        version_code = bundle_version_code  # Use the version code of the uploaded bundle

        # Update the track to create a draft release
        track_request = service.edits().tracks().update(
            packageName=package_name,
            editId=edit_id,
            track=track,
            body={'releases': [{'versionCodes': [version_code], 'status': 'draft'}]}
        )

        # version_code = 26
        release_body = [{
                'name':release_name,
                'status':'draft',
                'versionCodes':[version_code]
                }]

        track_request = service.edits().tracks().update(
                packageName=package_name,
                editId=edit_id,
                track=track,
                body={'releases': release_body}
                )

        response = track_request.execute()
        print(f"Draft release '{release_name}' created with version code {version_code}")

        # Commit the changes to finalize the edit
        commit_request = service.edits().commit(
            packageName=package_name,
            editId=edit_id
        )
        commit_request.execute()

        print("Changes committed and edit finalized.")


class RelGithub:

    def create_release(self, version, title, description, target_branch):
        """Create a new release on GitHub."""
        url = "https://api.github.com/repos/deku-messaging/Deku-SMS-Android/releases"
        data = {
            "tag_name": version,
            "name": title,
            "body": description,
            "target_commitish": target_branch,
            "draft": False,
            "prerelease":False,
            "generate_release_notes":False
        }

        github_token = get_github_token()

        print(github_token)

        headers = {"Authorization": "Bearer {}".format(github_token), 
                   "X-GitHub-Api-Version": "2022-11-28",
                   "Accept": "application/vnd.github+json"}

        response = requests.post(url, json=data, headers=headers)
        response.raise_for_status()

        print(response.status_code)
        return json.loads(response.text)

    def get_github_token(self):
        github_token = None
        with open('release.properties', 'r') as fd:
            lines = fd.readlines()

        for line in lines:
            if line.startswith('github_token'):
                github_token = line.split("=")[1].strip() 
                return github_token


    def upload_assets(self, upload_url, name, data_path):
        """Upload assets to a new release on GitHub."""

        github_token = get_github_token()
        print(github_token)

        headers = {'Content-Type': 'application/octet-stream', 
                   "Authorization": "Bearer {}".format(github_token), 
                   "X-GitHub-Api-Version": "2022-11-28",
                   "Accept": "application/vnd.github+json"}

        upload_url = re.sub(r"\{\?name,label}", "", upload_url)

        params = {
            'name': name,
            'label': name
        }

        with open(data_path, 'rb') as f:
            data = f.read()

        response = requests.post(upload_url, headers=headers, data=data, params=params)
        response.raise_for_status()

        print(response.status_code)
        return json.loads(response.text)

if __name__ == "__main__":
    """
    version = '25'
    title = '0.0.1'
    description = 'new release'
    target_branch = 'dev'
    """

    """
    version = sys.argv[1]
    title = sys.argv[2]
    description = sys.argv[3]
    target_branch = sys.argv[4]
    name = sys.argv[5]
    data_path = sys.argv[6]


    rel_github = RelGithub()
    res = rel_github.create_release(version, title, description, target_branch)
    upload_url = res['upload_url']
    print(upload_url)

    print(rel_github.upload_assets(upload_url, name, data_path))
    """

    rel_google = RelGooglePlaystore()
    creds_file = sys.argv[1] 
    release_id = 1
    edit_body = "Testing new release"
    bundle_file = sys.argv[2]
    rel_google.create_edit_for_draft_release(creds_file, release_id, edit_body, bundle_file)
