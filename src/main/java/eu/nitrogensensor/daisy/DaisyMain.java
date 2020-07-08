package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;
import eu.nitrogensensor.daisylib.remote.ExtractedContent;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "daisy", mixinStandardHelpOptions = true, version = "daisykørsel 0.9", showDefaultValues = true,
        description = "Kørsel af Daisy")
public class DaisyMain implements Callable
{
  @CommandLine.Parameters(index = "0", description = "server, run, remote eller testkørsel." )
  String kommando;

  @CommandLine.Option(names = {"-d", "--directory"}, description = "Hvor mappen med Daisy-filerne er", defaultValue = ".")
  String dir;

  @CommandLine.Parameters(index = "1", description = "Daisyfiler")
  List<String> daisyfiler;

  @CommandLine.Option(names = {"-o", "--outputdirectory"}, description = "Hvor skal resultatet skrives til", defaultValue = ".")
  String outputdirectory;

//  @CommandLine.Option(names = {"-p", "--daisy-executable-path"}, description = "Til lokal kørsel: Sti til Daisy executable", defaultValue = "/opt/daisy/bin/daisy")
//  private String stiTilDaisy;

  @CommandLine.Option(names = {"-u", "--remote-endpoint-url"},
          description = "Remote: URL til endpoint på server hvor Daisy-kørslerne udføres",
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
              daisyModels.add(new DaisyModel( dir, daisyfil));
          }
          ResultExtractor re = new ResultExtractor();
          re.addFile("daisy.log");

          if (remoteEndpointUrl !=null) DaisyRemoteExecution.setRemoteEndpointUrl(remoteEndpointUrl);
          ArrayList<ExtractedContent> res = DaisyRemoteExecution.runParralel(daisyModels, re, Paths.get(outputdirectory));
          System.out.println("res = " + res);
      }
      else throw new Exception("Ukendt kommando: "+kommando);
      return null;
  }
}
