# Chores Server

[![Deploy](https://github.com/ramitsuri/chores-server/actions/workflows/deploy.yml/badge.svg?branch=main)](https://github.com/ramitsuri/chores-server/actions/workflows/deploy.yml)

Backend server for the [Chores app](https://github.com/ramitsuri/chores-client) written in Kotlin (JVM) with PostgreSQL server.

## To deploy to GCP
```
./gradlew appengineDeploy
```

## To test locally if version that's going to be deployed will work
```
$env:GOOGLE_APPLICATION_CREDENTIALS="<path to credential json file>"
./gradlew appengineStage
cd .\build\staged-app\
java -jar choresserver-<version>-all.jar
```

## GitHub Actions Setup
- Go to [service accounts](https://console.cloud.google.com/iam-admin/serviceaccounts) on Google Cloud Console
- Create a new service account
- Give appropriate name and description
- Give the following roles
  - App Engine Deployer
  - App Engine Service Admin
  - Storage Object Creator
  - Storage Object Viewer
- Click Done
- Go to keys and add a new JSON key
- Download the key
- Create YAML file for GitHub Workflow
- Create Secrets for Project Id and JSON key that was downloaded earlier
- Paste the contents of the json file as value for the secret
- If gradle permission issue, run `git update-index --chmod=+x gradlew`