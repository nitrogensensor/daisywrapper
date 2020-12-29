
# Kørsel af Daisy


## Lokal kommandolinjetest uden for Docker

```
./gradlew jar
java -jar build/libs/daisy.jar testkørsel
java -jar build/libs/daisy.jar server
java -jar daisy.jar client -d src/test/resources/TestData/ Exercise01.dai
```

## Lokal test i Docker
Tjek Dockerfile

docker build  --tag=daisykoersel .
docker run -p 3210:3210 -t -i daisykoersel
Åbn http://localhost:3210



## Læg op på Google Cloud Run

### Førstegangs forberedelse
Følg https://cloud.google.com/run/docs/quickstarts/build-and-deploy

### Engangskald til at sætte hvor kørslerne udføres
gcloud config set run/region europe-north1

### Læg i drift / upload til Dockerbillede til Google Cloud Run
gcloud builds submit --tag gcr.io/nitrogensensor/daisykoersel && \
gcloud run deploy --image gcr.io/nitrogensensor/daisykoersel --platform managed --allow-unauthenticated daisykoersel


## Diverse noter
### Lagring i Google Cloud bucket (til kørselsfiler)

Oprette
gsutil mb -l europe-north1 gs://daisykoersel-arbejdsfiler/

Kopiere filer ind til skyen

gsutil cp README.md gs://daisykoersel-arbejdsfiler/
gsutil cp -r /slamkode/src/main/resources/geotiff-eksempler/ gs://daisykoersel-arbejdsfiler/

Liste filer:
gsutil ls -lr gs://daisykoersel-arbejdsfiler/

Kopiere fra skyen
gsutil cp -r gs://daisykoersel-arbejdsfiler/slamkode/src/main/resources/geotiff-eksempler/ .

gsutil du -h gs://daisykoersel-arbejdsfiler/

gsutil -m rm -rf  gs://daisykoersel-arbejdsfiler/slamkode/