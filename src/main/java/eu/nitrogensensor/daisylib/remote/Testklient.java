package eu.nitrogensensor.daisylib.remote;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;

class Testklient {
    public static void testkald() {
        Server.start();

        String url = Server.getUrl();
        HttpResponse response = Unirest.get(url).asString();
        System.out.println(response.getBody());

        System.out.println("sim:"+Unirest.post(url+"/sim").asString().getBody());

        response = Unirest.post(url+"/uploadZip")
//                .field("data", new File("/home/j/Hent/src.zip"))
                .field("data", new File("build.gradle"))  // burde være en ZIP-fil
                .asString();
        System.out.println(response.getBody());
        Server.stop();
    }

    public static void main(String[] args) {
        testkald();
    }
}
