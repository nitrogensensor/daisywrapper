package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "daisy", mixinStandardHelpOptions = true, version = "daisykørsel 0.9", showDefaultValues = true,
        description = "Kørsel af Daisy")
public class DaisyMain implements Callable
{
    private static final String VERSION = "0.901 (29 sept 2020 fejlfinding til Simon2)";
    @CommandLine.Parameters(index = "0", description = "server, run, remote eller testkørsel." )
  String kommando;

  @CommandLine.Option(names = {"-d", "--inputdirectory"}, description = "Mappen med Daisy-filerne, der skal køres", defaultValue = ".")
  String inputdirectory;

  @CommandLine.Parameters(index = "1..", description = "Daisyfil(er), der skal køres i mappen")
  List<String> daisyfiler;

  @CommandLine.Option(names = {"-r", "--replace"}, description = "Erstatninger der skal ske i daisyfilen før den køres. Hver erstatning består af et søgeudtryk og en erstatningsstreng adskilt af komma. Eksempler\n" +
          "-r _sand_,37.1   erstatter '_sand_' med '37.1'\n" +
          "-r _sand_:_humus_,10:90,20:80,30:70,40:60,50:50  giver 5 kørsler hvor sand stiger fra 10 til 50 og humus falder fra 90 til 50 i skridt af 10\n" +
          "-r '(stop *),(stop 2015 8 20)' sætter stoptidspunkt for simuleringen.")
  List<String> replace = new ArrayList<>();

  @CommandLine.Option(names = {"-o", "--outputdirectory"}, description = "Hvor skal resultatet skrives til", defaultValue = ".")
  String outputdirectory;

  @CommandLine.Option(names = {"-of", "--outputfil"}, description = "remote: Hvilke outputfiler skal gemmes (f.eks -of daisy.log)", defaultValue = ".")
  List<String> outputfiler;

  @CommandLine.Option(names = {"-oc", "--clean-csv"}, description = "normalisér CSV-filer (fjerner header og enheder)", defaultValue = "false")
  boolean cleanCsvOutput;

//  @CommandLine.Option(names = {"-p", "--daisy-executable-path"}, description = "Til lokal kørsel: Sti til Daisy executable", defaultValue = "/opt/daisy/bin/daisy")
//  private String stiTilDaisy;

  @CommandLine.Option(names = {"-u", "--remote-endpoint-url"},
          description = "remote: URL til endpoint på serveren, der udfører Daisy-kørslerne",
          defaultValue = "http://nitrogen.saluton.dk:3210")
  private String remoteEndpointUrl;

  public static void main(String[] args)  {
    System.out.println("hej fra DaisyMain version "+VERSION );
    int exitCode = new CommandLine(new DaisyMain()).execute(args);
    if (exitCode!=0) System.exit(exitCode);
  }

  @Override
  public Object call() throws Exception {
      if ("server".equals(kommando)) eu.nitrogensensor.daisylib.remote.Server.start();
      else if ("testkørsel".equals(kommando)) DaisyTestkoersel.main(null);
      else if ("remote".equals(kommando)) {
          ArrayList<DaisyModel> daisyModels = new ArrayList<>();
          for (String daisyfil : daisyfiler) {
              DaisyModel dm = new DaisyModel(inputdirectory, daisyfil);
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
                              DaisyModel dm1 = dm0.clon();
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
          for (String outputfil : outputfiler) re.addFile(outputfil);

          if (remoteEndpointUrl !=null) DaisyRemoteExecution.setRemoteEndpointUrl(remoteEndpointUrl);
          Map<String, ExtractedContent> res = DaisyRemoteExecution.runParralel(daisyModels, re);

          // TODO Hvis der opstår en serverfejl, f.eks. 'Too many open files' eller 'java.lang.IllegalArgumentException: Søgestreng ikke fundet: JORDLAG-30'
          // så skal den vises hos klienten!

          if (cleanCsvOutput) for (ExtractedContent ec : res.values()) cleanCsv(ec.fileContensMap);
          DaisyRemoteExecution.writeResults(res, Paths.get(outputdirectory));
          System.out.println("res = " + res);
      }
      else throw new Exception("Ukendt kommando: "+kommando);
      return null;
  }

    private void cleanCsv(HashMap<String, String> fileContensMap) {
        for (Map.Entry<String, String> fil : fileContensMap.entrySet()) {
            if (!fil.getKey().endsWith(".csv")) continue;
            String csv = fil.getValue();
            String[] csvsplit = csv.split("--------------------\n");
            if (csvsplit.length<2) continue;
            String rest = csvsplit[1];
            int n1 = rest.indexOf('\n');
            int n2 = rest.indexOf('\n', n1+1);
            csv = rest.substring(0, n1) + rest.substring(n2);
            fil.setValue(csv);
        }
    }
}
