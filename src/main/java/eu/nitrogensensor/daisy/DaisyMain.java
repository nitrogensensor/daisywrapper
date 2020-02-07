package eu.nitrogensensor.daisy;


import java.io.IOException;

public class DaisyMain
{

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("hej fra DaisyMain");

    DaisyInvoker daisyInvoke = new DaisyInvoker();
    daisyInvoke.invokeDaisy("/home/j/Projekter/NitrogenSensor/gitlab/nitrogensensor/daisy/src/test/resources/Taastrup 2019/dtu_model/Setup_DTU_Taastrup.dai");

  }
}
