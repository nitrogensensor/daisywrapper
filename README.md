# Daisywrapper
The Daisy wrapper wraps [Daisy](https://daisy.ku.dk) executions and runs them remotely.

This liberates the user of installing Daisy locally, and enables execution on a remote server, 
either on a self-hosted dedicated hardware or scaled out in the cloud, using Google Cloud Run.


### Getting the executable.

We regularly build a new executable, which is available at https://nitrogen.saluton.dk/resultat/diverse/daisy.jar.

If you want to compile it yourself you need to clone the repo and issue
```
./gradlew build
```
After compilation the wrapper is in daisy/build/libs/daisy.jar. 


## Usage - how to execute Daisy remotely

The wrapper is a command line tool. 
Assuming you that have daisy.jar in your current working directory and that there is 
a subdirectory called src/test/resources/TestData/ (relative to your current working directory) 
containing Exercise01.dai the following command
```
java -jar daisy.jar remote -d src/test/resources/TestData/ Exercise01.dai
``` 
will execute Daisy remtely and create a directory (Exercise01/) containing the result of the execution.

#### Options for remote execution

If run as 'remote' (client) usage is as follows:

```
java -jar daisy.jar client  [-chnvV] -d=<inputdirectory> [-o=<outputdirectory>] [-u=<remoteEndpointUrl>]
             [-of=<outputfiles>]... [-r=<replace>]... [<daisyfiles>...]
      [<daisyfiles>...]     Daisy fil(es) to be executed in the input directory

  -d, --inputdirectory      Input directory, containing the Daisy-file(s) to be executed

  -c, -oc, --clean-csv      All output files with a .csv suffix is reformatted to be valid CSV files (header and units are removed)

  -o, --outputdirectory     Where to write the result to. Default: .

  -of, --outputfile         Which output files to save (eg -of daisy.log). Default: . (the complete directory, with all files and directories is saved)

  -r, --replace             Replacements to be the daisy file before it is executed. 
                            Each substitution consists of a search term and a substitution string separated by commas. Examples:
                            -r _sand_,37.1   replaces '_sand_' with '37.1'
                            -r _sand_:_humus_,10:90,20:80,30:70,40:60,50:50  gives 5 runs where sand rises from 10 to 50 and humus falls from 90 to 50 in steps of 10
                            -r '(stop *),(stop 2015 8 20)' sets the stop time for the simulation.

  -u, --remote-endpoint-url URL to the endpoint of the server performing the Daisy execution. Default: http://nitrogen.saluton.dk:3210

  -v, --verbose             Print debugging information
  -V, --version             Print version information and exit.
  -h, --help                Show this help message and exit.

``` 


#### Options for the execution server 

If run as a server the usage is as follows:

```
java -jar daisy.jar server 

  -p, --daisy-executable-path Path to Daisy executable

  -n, --nice                  Run Daisy executable with lower scheduling priority

  -v, --verbose               Print debugging information

  -V, --version               Print version information and exit.
``` 

The server runs only on Linux (and possibly on Mac), 
If -p is not provided it assumes that Daisy is installed in /opt/daisy/.

If you have problems, please see the Dockerfile which gives a complete 
description on how to set up an execution server.




# BLABLA ikke færdigt herunder



```shell script
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