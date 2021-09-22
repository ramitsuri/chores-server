FROM arm32v7/openjdk
# FROM openjdk:8-jdk
EXPOSE 8080:8080
RUN mkdir /app
COPY ./build/install/com.ramitsuri.choresserver/ /app/
RUN mkdir /app/bin/data
WORKDIR /app/bin
CMD ["./com.ramitsuri.choresserver"]

# ./gradlew :docker:installDist
# docker build -t <application-name> .
# docker run -d -p 8080:8080 <application-name>

# docker logs <container-id>

# https://blog.alexellis.io/getting-started-with-docker-on-raspberry-pi/
# scp file.zip pi@192.168.1.210:Downloads // Or pi@raspberrypi:Downloads
# sudo rm -rf dir