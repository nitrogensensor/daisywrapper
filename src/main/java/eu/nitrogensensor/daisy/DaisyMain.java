package eu.nitrogensensor.daisy;


import eu.nitrogensensor.daisylib.DaisyModel;
import eu.nitrogensensor.daisylib.ResultExtractor;
import eu.nitrogensensor.daisylib.remote.DaisyRemoteExecution;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DaisyMain
{

  public static void main(String[] args) throws IOException {
    System.out.println("hej fra DaisyMain");

    if (args.length>0) {
      if ("server".equals(args[0])) {
        eu.nitrogensensor.daisylib.remote.Server.start();
        return;
      }
      if ("testkørsel".equals(args[0])) {
        DaisyTestkoersel.main(args);
        return;
      }
    }



  }
}
