package eu.nitrogensensor.daisylib.remote;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;

public class Testklient {
    public static void testkald() {
        String url = Server.url;
        HttpResponse response = Unirest.get(url).asString();
        System.out.println(response.getBody());


        System.out.println("sim:"+Unirest.post(url+"/sim").asString().getBody());



        response = Unirest.post(url+"/upload")
                .field("files", new File("/home/j/xxx"))
                .field("files", new File("/home/j/x.jpg"))
                .asString();
        System.out.println(response.getBody());
    }

    public static void main(String[] args) {
        testkald();
    }
}
