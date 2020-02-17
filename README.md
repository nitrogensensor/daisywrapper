# Kørsel af Daisy


## Lokal kommandolinjetest uden for Docker

Fra overmappen
./gradlew jar; java -jar daisy/build/libs/daisy.jar testkørsel

./gradlew jar; java -jar daisy/build/libs/daisy.jar server


../gradlew jar

Test lokalt
java -jar build/libs/daisy.jar testklient
java -jar build/libs/daisy.jar server

java -cp build/libs/daisy.jar eu.nitrogensensor.daisy.DaisyMain


java -jar build/libs/daisy.jar server
Åbn http://localhost:8080

## Lokal test i Docker
Tjek Dockerfile

docker build  --tag=daisykoersel .
docker run -p 8080:8080 -t -i daisykoersel
Åbn http://localhost:8080


docker run -p 8080:8080 -it daisykoersel . 
docker run -p 8080:8080 -d daisykoersel .

## Læg op på Google Cloud Run

### Førstegangs forberedelse
Følg https://cloud.google.com/run/docs/quickstarts/build-and-deploy

### Engangskald til at sætte hvor kørslerne udføres
gcloud config set run/region europe-north1

### Læg i drift / upload til Dockerbillede til Google Cloud Run
gcloud builds submit --tag gcr.io/nitrogensensor/daisykoersel 
gcloud run deploy --image gcr.io/nitrogensensor/daisykoersel --platform managed --allow-unauthenticated

