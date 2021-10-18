# To deploy to GCP
```
./gradlew appengineDeploy
```

# To test locally if version that's going to be deployed will work
```
$env:GOOGLE_APPLICATION_CREDENTIALS="<path to credential json file>"
./gradlew appengineStage
cd .\build\staged-app\
java -jar choresserver-<version>-all.jar
```
