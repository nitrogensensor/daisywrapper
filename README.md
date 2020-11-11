# Daisywrapper
Daisywrapper wraps [Daisy](https://daisy.ku.dk) executions and runs them remotely.
This liberates the user of installing Daisy locally, and enables execution on a remote server, 
either on a self-hosted dedicated hardware or scaled indefinitly in the cloud, using Google Cloud Run.


# Usage
.dai-files:
```sh
DaisyModel d = new DaisyModel("C:\Program Files\Daisy 5.72\exercises\", "Exercise01.dai");
d.replace("(stop *)", "(stop 1994 8 31)");   // Set stop date
d.run();



from pydaisy.Daisy import *
d = DaisyModel(r'C:\Program Files\Daisy 5.72\exercises\Exercise01.dai')
print(d.starttime)
dry_bulk_density = d.Input['defhorizon'][0]['dry_bulk_density'].getvalue()
d.Input['defhorizon'][0]['dry_bulk_density'].setvalue(1.1*dry_bulk_density)
d.save_as(r'C:\Program Files\Daisy 5.72\exercises\Exercise01_new.dai')
DaisyModel.path_to_daisy_executable =  r'C:\Program Files\Daisy 5.72\bin\Daisy.exe'
d.run()
```

.dlf- and .dwf-files:
```sh
from datetime import datetime
from pydaisy.Daisy import *
dlf = DaisyDlf(r'C:\Program Files\Daisy 5.72\exercises\Taastrup6201.dwf')
pandasdata = dlf.Data
numpy_data = dlf.numpydata
i=dlf.get_index(datetime(1962,4,14))
pandasdata['Precip'][i]=10
dlf.save(r'C:\Program Files\Daisy 5.72\exercises\Taastrup6201_saved.dwf')
```


Parallel runs:
```sh
if __name__ == '__main__':
    from pydaisy.Daisy import *
    DaisyModel.path_to_daisy_executable =  r'C:\Program Files\Daisy 5.72\bin\Daisy.exe'
    daisyfiles =[r'c:\daisy\model1\setup.dai', r'c:\daisy\model2\setup.dai', r'c:\daisy\model3\setup.dai']
    run_many(daisyfiles, NumberOfProcesses=3)
```
The code above will run the three daisy-simulations in parallel.


# Kørsel af Daisy


## Lokal kommandolinjetest uden for Docker

Fra overmappen
cd ..
./gradlew jar; 
java -jar daisy/build/libs/daisy.jar testkørsel
java -jar daisy/build/libs/daisy.jar server
java -jar daisy/build/libs/daisy.jar remote -u http://localhost:8080 -d /home/j/Hent/mads replaced_Sim_4_RefSim.dai -o xxx
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
gcloud builds submit --tag gcr.io/nitrogensensor/daisykoersel && \
gcloud run deploy --image gcr.io/nitrogensensor/daisykoersel --platform managed --allow-unauthenticated daisykoersel

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