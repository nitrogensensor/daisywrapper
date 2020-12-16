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


## Usage - how to execute Daisy on a remote server

The wrapper is a command line tool. 

Assuming that
  - you have daisy.jar in your current working directory and that 
  - there is a subdirectory (relative to your current working directory) 
called src/test/resources/TestData/
  - it contains a Daisy script called Exercise01.dai

then the following command
```
java -jar daisy.jar remote -d src/test/resources/TestData/ Exercise01.dai
``` 
will execute Daisy remotely and create a directory (Exercise01/) containing the result of the execution.

### Options for remote execution

If run as client (i.e. using a remote server) the usage is as follows:

```
java -jar daisy.jar client [-chnvV] -d=<inputdirectory> [-o=<outputdirectory>] [-u=<remoteEndpointUrl>]
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

  -u, --remote-endpoint-url URL to the endpoint of the server performing the Daisy execution. 
                            Default: http://nitrogen.saluton.dk:3210

  -v, --verbose             Print debugging information
  -V, --version             Print version information and exit.
  -h, --help                Show this help message and exit.

``` 
### Example

```
java -jar daisy.jar remote -r '(stop *),(stop 2015 8 20)' -d src/test/resources/TestData/ Exercise01.dai
``` 


## Usage - how to set up a Daisy execution server

To run an execution server the usage is as follows:

```
java -jar daisy.jar server 

  -p, --daisy-executable-path Path to Daisy executable
                              Default: /opt/daisy/bin/daisy

  -n, --nice                  Run Daisy executable with lower scheduling priority

  -v, --verbose               Print debugging information

  -V, --version               Print version information and exit.
``` 

The server runs only on Linux (and possibly on Mac), 
If -p is not provided it assumes that Daisy is installed in the default 
directory on Linux (which is /opt/daisy/).

If you have problems, please see the Dockerfile which gives a complete 
description on how to set up an execution server.




# Usage as a Java library



```
import eu.nitrogensensor.daisylib.*;
...

DaisyModel d = new DaisyModel("daisy/src/test/resources/TestData", "Exercise01.dai");
d.replace("(stop *)", "(stop 1995 1 1)");   // Set stop date
d.run();
```
This will run Daisy directly in the source directory and the 
output files will be written there as well by Daisy.

It only works if you have Daisy installed.

(NOTE: It is not recommended mix source and output - take a 
copy of the entire source directory, using
`copyToDirectory()`. 
This operation is extremely fast and does not take up space as 
it is using Unix symbolic links. It only works on Linux and possibly Mac).

## Remote parallel runs

You can run Daisy without installing it by doing remote parallel runs.

To do multiple runs we create list of copies of the original DaisyModel.
Each copy has its own uniqe ID and replacements.
Assuming that the text `(dry_bulk_density 1.53 [g/cm^3])` is somewhere in 
the `Exercise01.dai` file we can replace that with values 1.40 up to 1.50 using:

```
DaisyModel copy =d.createCopy()
            .setId("dbd_1.40")
            .replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.40 [g/cm^3])");
daisyModels.add(copy);

daisyModels.add(d.createCopy().setId("dbd_1.42").replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.42 [g/cm^3])"));
daisyModels.add(d.createCopy().setId("dbd_1.44").replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.44 [g/cm^3])"));
daisyModels.add(d.createCopy().setId("dbd_1.46").replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.46 [g/cm^3])"));
daisyModels.add(d.createCopy().setId("dbd_1.48").replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.48 [g/cm^3])"));
daisyModels.add(d.createCopy().setId("dbd_1.50").replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density 1.50 [g/cm^3])"));
```

Now we can run the models remotely using
```
DaisyRemoteExecution.runParralel(daisyModels, Paths.get("tmp/remote_result"))
```

Inside `tmp/remote_result` will be a subdirectory for each ID (`dbd_1.40` to `dbd_1.50` )


## Local parallel runs

Use `copyToDirectory()` to avoid getting the directories mixed up as mentioned above:

```
ArrayList<DaisyModel> daisyModels = new ArrayList<>();
for (double dry_bulk_density=1.40; dry_bulk_density<1.60; dry_bulk_density+=0.02) {
    DaisyModel copy = d.createCopy()
            .setId("dbd_"+dry_bulk_density)
            .copyToDirectory(Paths.get("tmp/local_result/dbd_"+dry_bulk_density))
            .replace("(dry_bulk_density 1.53 [g/cm^3])", "(dry_bulk_density "+dry_bulk_density+" [g/cm^3])");
    daisyModels.add(copy);
}

DaisyExecution.runParralel(daisyModels)
```
The code above will run the ten daisy-simulations in parallel - as many at the 
same time as the number of cores in your PC.




# Kørsel af Daisy


## Lokal kommandolinjetest uden for Docker

Fra overmappen
```
cd ..
./gradlew jar; 
java -jar daisy/build/libs/daisy.jar testkørsel
java -jar daisy/build/libs/daisy.jar server
java -jar daisy/build/libs/daisy.jar remote -u http://localhost:8080 -d /home/j/Hent/mads replaced_Sim_4_RefSim.dai -o xxx
```
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