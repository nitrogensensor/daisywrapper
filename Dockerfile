### Byggeprocesen
# Det er
#FROM gradle:5.1 as builder
FROM gradle:5.5.1-jdk11 as builder
#FROM openjdk:12-alpine as builder

# Kun Daisy er nødvendig
#COPY . .

COPY build.gradle .
COPY settings.gradle .
COPY daisy daisy

## Brug gradle-wrapperen der er med i projektet
#COPY gradlew .
#COPY gradle gradle
#RUN ./gradlew clean build --no-daemon

# Spring over tests ( -x test) - Daisy er ikke installeret på byggemaskinen
RUN gradle clean build -x test --no-daemon

# Under kørsel
FROM adoptopenjdk/openjdk11:jre

# Bruger vi FROM adoptopenjdk/openjdk11 uden :jre fylder det 140 MB mere - se
# https://console.cloud.google.com/gcr/images/nitrogensensor/GLOBAL/daisykoersel?project=nitrogensensor&folder&organizationId&gcrImageListsize=30
# Og lokalt fylder det 200 MB mindre - tjek det selv med
# 219M:  docker run --rm -it adoptopenjdk/openjdk11:jre du -h /
# 414M:  docker run --rm -it adoptopenjdk/openjdk11 du -h /


RUN apt update && apt install libcxsparse3 -y
RUN curl https://daisy.ku.dk/download/daisy_5.88_amd64.deb > daisy.deb && apt install ./daisy.deb && rm -f daisy.deb

# Kopier JAR fra builder stage.
COPY --from=builder /home/gradle/daisy/build/libs/daisy.jar /daisy.jar
COPY daisy daisy


# Start serveren
#CMD [ "java", "-jar", "/daisy.jar", "testkørsel" ]
CMD [ "java", "-jar", "/daisy.jar", "server" ]


# docker run -p 8080:8080 -t -i daisykoersel