unzip ChoresServer.zip
cd ChoresServer/
chmod +x gradlew
cd build/install/com.ramitsuri.choresserver/bin/
chmod +x com.ramitsuri.choresserver
cd ../../../../
docker build -t $1 .
docker run -d -p 80:8080 $1