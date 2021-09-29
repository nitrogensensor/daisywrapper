# Daisywrapper
The Daisy wrapper wraps [Daisy](https://daisy.ku.dk) executions and runs them remotely.

This liberates the user of installing Daisy locally, and enables execution on a remote server, 
either on a self-hosted dedicated hardware or scaled out in the cloud, using Google Cloud Run.

This module wraps the daisy execution in a command-line interface CLI for
quickly simulating many different but similar simulations. The module contains a
CLI interface to a client which can run daisy locally or remotely as well as a
server which can serve the requests of the client.

# Quickstart

Here is what you need to get started.
In-depth documentation, inclusive how to run a server and how to use it as a Java library, [is available](documentation)

### Getting the executable.

Download https://daisy.nitrogensensor.eu/resultat/diverse/daisy.jar and save it in a known place on your computer.


## Usage - how to execute Daisy on a remote server

The wrapper is a command line tool. 

Assuming that
  - you have daisy.jar in your current working directory, and
  - there is a subdirectory (relative to your current working directory) 
called src/test/resources/TestData/, and
  - it contains a Daisy script called Exercise01.dai

then the following command
```
java -jar daisy.jar client -d src/test/resources/TestData/ Exercise01.dai
``` 
will execute Daisy remotely and create a directory (Exercise01/) containing the result of the execution.

### Options for remote execution

When you run is as client (i.e. using a remote server) the usage is as follows:

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

[comment]: <> (                            -r '&#40;stop *&#41;,&#40;stop 2015 8 20&#41;' sets the stop time for the simulation.)

  -u, --remote-endpoint-url URL to the endpoint of the server performing the Daisy execution. 
                            Default: http://daisy.nitrogensensor.eu:3210

  -v, --verbose             Print debugging information
  -V, --version             Print version information and exit.
  -h, --help                Show this help message and exit.

``` 
### Example

```
java -jar daisy.jar remote -r '(stop *),(stop 2008 8 20)' -d src/test/resources/TestData/ Exercise01.dai
``` 


