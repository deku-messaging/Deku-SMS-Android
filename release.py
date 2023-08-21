#!/usr/bin/python3
import re
import requests
import json
import sys

def create_release(version, title, description, target_branch):
    """Create a new release on GitHub."""
    url = "https://api.github.com/repos/deku-messaging/Deku-SMS-Android/releases"
    data = {
        "tag_name": version,
        "name": title,
        "body": description,
        "target_commitish": target_branch,
        "draft": True,
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

def get_github_token():
    github_token = None
    with open('release.properties', 'r') as fd:
        lines = fd.readlines()

    for line in lines:
        if line.startswith('github_token'):
            github_token = line.split("=")[1].strip() 
            return github_token


def upload_assets(upload_url, name, data_path):
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

    version = sys.argv[1]
    title = sys.argv[2]
    description = sys.argv[3]
    target_branch = sys.argv[4]
    name = sys.argv[5]
    data_path = sys.argv[6]


    res = create_release(version, title, description, target_branch)
    upload_url = res['upload_url']
    print(upload_url)

    print(upload_assets(upload_url, name, data_path))
