# Daisywrapper
The Daisy wrapper wraps [Daisy](https://daisy.ku.dk) executions and runs them remotely.

TLDR: ***You don't need to install/run Daisy on your own PC anymore - your simulations are better executed in parrallel on remote powerful servers***.

The servers can either be self-hosted on dedicated hardware or scaled out in the cloud.
The default server runs at DTU Diplom and can run 16 simulations in parrallel, and is free to use (please don't abuse). 
If you need more the cloud solution enables you to run several thousand simulations in parralel, only paying for the actual CPU time used.

Please see in-depth [documentation](documentation) for
- how to set up your own server, 
- how to set up the Google Cloud Run solution, scaling and pricing
- and how to use it as a Java library

Also read this slideset: https://bit.ly/daisywrapper


# Quickstart - how to execute Daisy on a remote server

Here is what you need to get started.

If you use R there is an [R Studio](https://www.rstudio.com/) example notebook that [does it all for you](src/test/resources/TestData/rstudio_notebook.Rmd).


## Getting the wrapper.

You will need to install Java (JRE).

Then download https://daisy.nitrogensensor.eu/resultat/diverse/daisy.jar and save it in a known place on your computer.


## Usage

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
                            -r '(stop *),(stop 2015 8 20)' sets the stop time for the simulation.

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


