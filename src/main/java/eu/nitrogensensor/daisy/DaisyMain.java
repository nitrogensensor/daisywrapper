package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisy.demo.DaisyTaastrup2019TestRun;
import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.Utils;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "daisy", mixinStandardHelpOptions = true, showDefaultValues = true, usageHelpWidth = 120)
public class DaisyMain implements Callable
{
    public static final String VERSION = "v1.0 (18 okt 2021)";
//    @CommandLine.Parameters(index = "0", description = "server, run, remote eller testkørsel." )
    @CommandLine.Parameters(index = "0", description = "remote/client to run Daisy remotely, or 'server' to start a calculation server" )
  String command;

  //@CommandLine.Option(names = {"-d", "--inputdirectory"}, description = "Mappen med Daisy-filerne, der skal køres", defaultValue = ".")
  @CommandLine.Option(names = {"-d", "--inputdirectory"}, description = "Input directory, containing the Daisy-file(s) to be executed")
  String inputdirectory;

//  @CommandLine.Parameters(index = "1..", description = "Daisyfil(er), der skal køres i mappen")
  @CommandLine.Parameters(index = "1..", description = "Daisy fil(es) to be executed in the input directory")
  List<String> daisyfiles;

/*
  @CommandLine.Option(names = {"-r", "--replace"}, description = "Erstatninger der skal ske i daisyfilen før den køres. Hver erstatning består af et søgeudtryk og en erstatningsstreng adskilt af komma. Eksempler\n" +
          "-r _sand_,37.1   erstatter '_sand_' med '37.1'\n" +
          "-r _sand_:_humus_,10:90,20:80,30:70,40:60,50:50  giver 5 kørsler hvor sand stiger fra 10 til 50 og humus falder fra 90 til 50 i skridt af 10\n" +
          "-r '(stop *),(stop 2015 8 20)' sætter stoptidspunkt for simuleringen.")

 */
  @CommandLine.Option(names = {"-r", "--replace"},
          description = "Replacements to be the daisy file before it is executed. " +
                  "Each substitution consists of a search term and a substitution string separated by commas. Examples:\n" +
          "-r _sand_,37.1   replaces '_sand_' with '37.1'\n" +
          "-r _sand_:_humus_,10:90,20:80,30:70,40:60,50:50  gives 5 runs where sand rises from 10 to 50 and humus falls from 90 to 50 in steps of 10\n" +
          "-r '(stop *),(stop 2015 8 20)' sets the stop time for the simulation.")
  List<String> replace = new ArrayList<>();

//  @CommandLine.Option(names = {"-o", "--outputdirectory"}, description = "Hvor skal resultatet skrives til", defaultValue = ".")
  @CommandLine.Option(names = {"-o", "--outputdirectory"}, description = "Where to write the result to", defaultValue = ".", showDefaultValue = CommandLine.Help.Visibility.ON_DEMAND)
  String outputdirectory;

//  @CommandLine.Option(names = {"-of", "--outputfil"}, description = "remote: Hvilke outputfiler skal gemmes (f.eks -of daisy.log)", defaultValue = ".")
  @CommandLine.Option(names = {"-of", "--outputfile"}, description = "remote: Which output files to save (eg -of daisy.log)", defaultValue = ".", showDefaultValue = CommandLine.Help.Visibility.ON_DEMAND)
  List<String> outputfiler;

//  @CommandLine.Option(names = {"-c", "-oc", "--clean-csv"}, description = "normalisér CSV-filer (fjerner header og enheder)", defaultValue = "false")
@CommandLine.Option(names = {"-c", "-oc", "--clean-csv"}, description = "all output files with a .csv suffix is reformatted to be valid CSV files (header and units are removed)", defaultValue = "false")
  boolean cleanCsvOutput;


    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Print debugging information", defaultValue = "false")
    boolean verbose;

    @CommandLine.Option(names = {"-n", "--nice"}, description = "Run Daisy executable with lower scheduling priority", defaultValue = "false")
    boolean nice;

//    @CommandLine.Option(names = {"-p", "--daisy-executable-path"}, description = "Til lokal kørsel: Sti til Daisy executable" )
    @CommandLine.Option(names = {"-p", "--daisy-executable-path"}, description = "Path to Daisy executable (for local execution)" )
    private String stiTilDaisy=null;

  @CommandLine.Option(names = {"-u", "--remote-endpoint-url"},
//          description = "remote: URL til endpoint på serveren, der udfører Daisy-kørslerne",
          description = "remote: URL to the endpoint of the server performing the Daisy execution",
          defaultValue = "http://daisy.nitrogensensor.eu:3210")
  private String remoteEndpointUrl;

  public static void main(String[] args)  {
    System.out.println("Daisywrapper version "+VERSION );
    System.out.println("Copyright 2021 nitrogensensor.eu & Jacob Nordfalk - github.com/nitrogensensor/daisywrapper");
    int exitCode = new CommandLine(new DaisyMain()).execute(args);
    if (exitCode!=0) System.exit(exitCode);
  }

  @Override
  public Object call() throws Exception {
      Utils.debug = verbose;
      if (nice) DaisyModel.nice_daisy = nice;
      DaisyRemoteExecution.setRemoteEndpointUrl(remoteEndpointUrl);
      if (stiTilDaisy!=null) DaisyModel.path_to_daisy_executable = stiTilDaisy;

      if ("server".equals(command)) eu.nitrogensensor.daisylib.remote.Server.start();
      else if ("testkørsel".equals(command)) DaisyTaastrup2019TestRun.main(null);
      else if ("remote".equals(command) || "client".equals(command)  ) {
          if (inputdirectory==null) {
              if (daisyfiles==null || daisyfiles.size()!=1) {
                  System.err.println("Please use -d to provide the input directory");
                  return null;
              }
              System.err.println("NOTE: You didn't provide an input directory. I am assuming it is in "+ daisyfiles.get(0)+"'s containing folder.");
              Path sti = Paths.get(daisyfiles.get(0));
              if (sti.getParent()!=null) inputdirectory = sti.getParent().toString(); else inputdirectory = ".";
              daisyfiles.clear();
              daisyfiles.add(sti.getFileName().toString());
              System.err.println("NOTE: Next time, use: -d "+inputdirectory+" "+ daisyfiles.get(0));
              System.err.println();
          }
          ArrayList<DaisyModel> daisyModels = new ArrayList<>();
          for (String daisyfil : daisyfiles) {
              DaisyModel dm = new DaisyModel(inputdirectory, daisyfil);
              //  TODO resultmappe ignoreres i opload. Lige nu oploades result med, fordi den ligger i inputmappen og derfor kan en gammel kørsel ikke genbruges
              //dm.ignorérDenneMappe(outputdirectory);
              if (daisyfil.endsWith(".dai")) daisyfil = daisyfil.substring(0, daisyfil.length()-4);
              dm.setId(daisyfil);
              daisyModels.add(dm);

              for (String rElem : replace) {
                  // System.out.println("rElem = " + rElem);
                  String[] søgErstat = rElem.split(",");
                  if (søgErstat.length < 2) throw new IllegalArgumentException(
                          "Fejl i "+replace+" for "+rElem+". Formatet er: \nsøg,erstat  - med komma imellem, eller"
                                  +"\nsøgA:søgB,erstat1A:erstat1B,erstat2A:erstat2B,erstat3A,erstat3B  - med komma imellem.");
                  if (søgErstat.length == 2) dm.replace(søgErstat[0],søgErstat[1]);
                  else {
                      ArrayList<DaisyModel> nydaisyModels = new ArrayList<>();
//                      System.out.println("Arrays.toString(søgErstat) = " + Arrays.toString(søgErstat));
                      String[] nøgler = søgErstat[0].split(":");
//                      System.out.println("Arrays.toString(nøgler) = " + Arrays.toString(nøgler));
//                      System.out.println("daisyModels00 = " + daisyModels);
                      for (DaisyModel dm0 : daisyModels) {
                          for (int i=1; i<søgErstat.length; i++) {
                              DaisyModel dm1 = dm0.createCopy();
                              String[] værdier = søgErstat[i].split(":");
//                              System.out.println("Arrays.toString(nøgler) = " + Arrays.toString(nøgler));
                              if (nøgler.length!=værdier.length) throw new IllegalArgumentException("Fejl i "+rElem+". Formatet er søgA:søgB,erstat1A:erstat1B,erstat2A:erstat2B,erstat3A,erstat3B  - med komma imellem. Du har "+nøgler.length+ " nøgler "+Arrays.asList(nøgler)+" , men " +værdier.length+" værdier "+Arrays.asList(værdier));
                              for (int j = 0; j < nøgler.length; j++) {
                                  dm1.replace(nøgler[j], værdier[j]);
                              }
                              dm1.setId(dm0.getId()+"_"+søgErstat[i]);
                              nydaisyModels.add(dm1);
                          }
                      }
                      daisyModels = nydaisyModels;
                  }
              }
          }

          //TODO Måske dette burde slettes? det giver ofte et output der er så langt og scroller så hurtigt at man
          // ikke kan undgå at miste meget af den information der printes.
          // Okay, det tog flere minutter at printe før jeg stoppe. Skrevet af Mads august 2020
          //System.out.println("daisyModels = ");
          //daisyModels.forEach(System.out::println);

          ResultExtractor re = new ResultExtractor();
          re.cleanCsvOutput = cleanCsvOutput;
          for (String outputfil : outputfiler) re.addFile(outputfil);

          if (remoteEndpointUrl !=null) DaisyRemoteExecution.setRemoteEndpointUrl(remoteEndpointUrl);
          Map<String, ExtractedContent> res = DaisyRemoteExecution.runParralel(daisyModels, re, Paths.get(outputdirectory));

          // TODO Hvis der opstår en serverfejl, f.eks. 'Too many open files' eller 'java.lang.IllegalArgumentException: Søgestreng ikke fundet: JORDLAG-30'
          // så skal den vises hos klienten!

          if (verbose) System.out.println("res = " + res);
      }
      else throw new Exception("Ukendt kommando: "+ command);
      return null;
  }

}
