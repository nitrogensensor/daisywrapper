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
/*
  @CommandLine.Option(names = {"-rr", "--replace-repeat"}, description = "Gentagelse af kørslen med forskellige erstatninger." +
  List<String> repeatReplace = new ArrayList<String>();

  @CommandLine.Option(names = {"-rc", "--replicate-replace"}, description = "Replikering af kørslen med forskellige erstatninger. Hver består af et søgeudtryk og et antal erstatningsstrenge adskilt af komma." +
          "Formatet er: søg,erstat1,erstat2,erstat3. Er der flere sæt replikerede erstatninger multipliceres de. Eksempelvis giver nedenstående i alt 25 kørsler:\n" +
          "-rr _sand_,0,10,20,30,40 -rr _humus_,50,60,70,80,90")
  List<String> replicateReplace = new ArrayList<String>();
*/
  @CommandLine.Option(names = {"-o", "--outputdirectory"}, description = "Hvor skal resultatet skrives til", defaultValue = ".")
  String outputdirectory;

  @CommandLine.Option(names = {"-of", "--outputfil"}, description = "remote: Hvilke outputfiler skal gemmes", defaultValue = "daisy.log")
  List<String> outputfiler;

  @CommandLine.Option(names = {"-oc", "--clean-csv"}, description = "normaliser CSV-filer (fjerner header og enheder)", defaultValue = "false")
  boolean cleanCsvOutput;

//  @CommandLine.Option(names = {"-p", "--daisy-executable-path"}, description = "Til lokal kørsel: Sti til Daisy executable", defaultValue = "/opt/daisy/bin/daisy")
//  private String stiTilDaisy;

  @CommandLine.Option(names = {"-u", "--remote-endpoint-url"},
          description = "remote: URL til endpoint på serveren, der udfører Daisy-kørslerne",
          defaultValue = "http://nitrogen.saluton.dk:3210")
  private String remoteEndpointUrl;

  public static void main(String[] args)  {
    System.out.println("hej fra DaisyMain");
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
                              if (nøgler.length!=værdier.length) throw new IllegalArgumentException("Fejl i "+replace+" for "+rElem+". Formatet er søgA:søgB,erstat1A:erstat1B,erstat2A:erstat2B,erstat3A,erstat3B  - med komma imellem.");
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
          System.out.println("daisyModels = " + daisyModels);
/*
          for (String elem : repeatReplace) {
              ArrayList<DaisyModel> nydaisyModels = new ArrayList<>();
              String[] søgErstat = elem.split(",");
              if (søgErstat.length < 3) throw new IllegalArgumentException("Fejl i "+repeatReplace+" for "+elem+": Formatet er: søgA:søgB,erstat1A:erstat1B,erstat2A:erstat2B,erstat3A,erstat3B  - med komma imellem.");

              for (DaisyModel dm0 : daisyModels) {
                  for (int i=1; i<søgErstat.length; i++) {
                      DaisyModel dm1 = dm0.clon();
                      dm1.replace(søgErstat[0], søgErstat[i]);
                      dm1.setId(dm0.getId()+","+søgErstat[i]);
                      nydaisyModels.add(dm1);
                  }
              }
              daisyModels = nydaisyModels;
          }


          for (String elem : replicateReplace) {
              ArrayList<DaisyModel> nydaisyModels = new ArrayList<>();
              String[] søgErstat = elem.split(",");
              if (søgErstat.length < 3) throw new IllegalArgumentException("Fejl i "+replicateReplace+" for "+elem+": Formatet er: søg,erstat1,erstat2,erstat3  - med komma imellem.");

              for (DaisyModel dm0 : daisyModels) {
                  for (int i=1; i<søgErstat.length; i++) {
                      DaisyModel dm1 = dm0.clon();
                      dm1.replace(søgErstat[0], søgErstat[i]);
                      dm1.setId(dm0.getId()+","+søgErstat[i]);
                      nydaisyModels.add(dm1);
                  }
              }
              daisyModels = nydaisyModels;
          }
*/

          ResultExtractor re = new ResultExtractor();
          for (String outputfil : outputfiler) re.addFile(outputfil);

          if (remoteEndpointUrl !=null) DaisyRemoteExecution.setRemoteEndpointUrl(remoteEndpointUrl);

          Map<String, ExtractedContent> res = DaisyRemoteExecution.runParralel(daisyModels, re);

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
