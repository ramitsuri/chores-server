# Chores Server

[![Deploy](https://github.com/ramitsuri/chores-server/actions/workflows/deploy.yml/badge.svg?branch=main)](https://github.com/ramitsuri/chores-server/actions/workflows/deploy.yml)

Backend server for the [Chores app](https://github.com/ramitsuri/chores-client) written in Kotlin (JVM) with PostgreSQL server.

## Creating a new release
- When ready to release a new version, update the version in `build.gradle`
- Update `entrypoint` value in `src/main/appengine/app.yaml` to correspond to the new version
- Commit code 
- Tag the commit in the format `v<version>`
```
git tag -a "v0.0.13" -m "Tagging v0.0.13"
```
- Push the tag. GitHub Actions will auto deploy the app
```
git push origin v0.0.13
```

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

## GH Actions deploy fails
If error message on GH Action logs is 
```
Execution failed for task ':appengineDeploy'.
> com.google.cloud.tools.appengine.AppEngineException: com.google.cloud.tools.appengine.operations.cloudsdk.process.ProcessHandlerException: com.google.cloud.tools.appengine.AppEngineException: Non zero exit: 1
```
And in gCloud build logs, build step is failing with 
```
ERROR: failed to initialize analyzer: getting previous image: getting config file for image "us.gcr.io/chores-326817/app-engine-tmp/app/default/ttl-18h:latest
```
Then fix is to go to https://console.cloud.google.com/gcr, delete previous images and then rebuild on gCloud console and then retrigger the GH action that failed. 
