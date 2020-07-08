package eu.nitrogensensor.daisylib.remote.google_cloud_storage;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GemOgHentArbejdsfiler {
    private static Storage storage;
    private static Bucket bucket;

    static  {

        try {
            String jsonPath = "/home/j/Projekter/NitrogenSensor/gitlab/nitrogensensor/daisy/daisykørsel-arbejdsfiler.json";
            // You can specify a credential file by providing a path to GoogleCredentials.
            // Otherwise credentials are read from the GOOGLE_APPLICATION_CREDENTIALS environment variable.
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        } catch (IOException e) {
            // hvis vi ikke kører på Jacobs PC, så kører vi nok på en server, f.eks. nitrogen.saluton.dk eller oppe i Cloud Run ;-)
            System.out.println("Dette er ikke Jacobs PC: "+e);
            storage = StorageOptions.getDefaultInstance().getService();
        }
        bucket = storage.get("daisykoersel-arbejdsfiler");
    }



    public static void main(String... args) throws Exception {

        // Instantiates a client
        Page<Bucket> buckets = storage.list();
        for (Bucket bucket : buckets.iterateAll()) {
            System.out.println(bucket.toString());
        }


        for (Blob b : bucket.list(Storage.BlobListOption.currentDirectory()).iterateAll()) {
            System.out.println(b.toString());
        }

        Blob b = bucket.get("README.md");
        b.downloadTo(Paths.get("/tmp/xxx.txt"));


        System.out.printf("Bucket %s created.%n", bucket.getName());

        /*
        list.

        BlobInfo.newBuilder();

        Storage.CopyRequest cr = Storage.CopyRequest.of();
        CopyWriter cw = storage.copy();
        // The name for the new bucket
        String bucketName = args[0];  // "my-new-bucket";

        // Creates the new bucket
        Bucket bucket = storage.create(BucketInfo.of(bucketName));

         */
    }

    public static void gem(InputStream content, String batchId) {
        bucket.create(batchId, content);
        //System.out.println("GemOgHentArbejdsfiler GEM id "+batchId+": "+f+" på "+f.toFile().length());
    }

    public static Path hent(String batchId) throws IOException {
        Blob blob = bucket.get(batchId);
        Path f = Files.createTempFile("GemOgHentArbejdsfiler", "hent");
        blob.downloadTo(f);
        System.out.println("GemOgHentArbejdsfiler HENT id "+batchId+": "+f+" på "+f.toFile().length());
        return f;
    }
}
