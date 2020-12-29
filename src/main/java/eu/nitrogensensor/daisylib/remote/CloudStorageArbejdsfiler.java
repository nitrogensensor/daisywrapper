package eu.nitrogensensor.daisylib.remote;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

class CloudStorageArbejdsfiler {
    private Storage storage;
    private Bucket bucket;

    public CloudStorageArbejdsfiler() {
        try {
            String jsonPath = "daisykørsel-arbejdsfiler-serviceAccountCredentials.json";
            // You can specify a credential file by providing a path to GoogleCredentials.
            // Otherwise credentials are read from the GOOGLE_APPLICATION_CREDENTIALS environment variable.
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        } catch (IOException e) {
            // hvis vi ikke kører på Jacobs PC, så kører vi nok på en server, f.eks. nitrogen.saluton.dk eller oppe i Cloud Run ;-)
            System.out.println("Dette er IKKE Jacobs PC, så vi er på nitrogen.saluton.dk eller Cloud Run: "+e);
            storage = StorageOptions.getDefaultInstance().getService();
        }

        try {
            bucket = storage.get("daisykoersel-arbejdsfiler");
        } catch (StorageException e) {
            System.out.println("Der er IKKE adgang til Cloud Storage: "+e);
        }
    }


    public void gem(InputStream content, String batchId) {
        if (bucket==null) return;
        bucket.create(batchId, content);
        //System.out.println("GemOgHentArbejdsfiler GEM id "+batchId+": "+f+" på "+f.toFile().length());
    }

    public Path hent(String batchId) throws IOException {
        if (bucket==null) return null;
        Blob blob = bucket.get(batchId);
        if (blob==null) return null;
        Path f = Files.createTempFile("GemOgHentArbejdsfiler", "hent");
        blob.downloadTo(f);
        System.out.println("GemOgHentArbejdsfiler HENT id "+batchId+": "+f+" på "+f.toFile().length());
        return f;
    }


    /** Sletter alle kørsler der er ældre end 5 dage */
    private void oprydning() {
        // Instantiates a client
        Page<Bucket> buckets = storage.list();
        for (Bucket bucket : buckets.iterateAll()) {
            System.out.println(bucket.toString());
        }

        long nu = System.currentTimeMillis();
        for (Blob b : bucket.list(Storage.BlobListOption.currentDirectory()).iterateAll()) {
            System.out.println(b.toString());
            if (b.getCreateTime()==null) continue;
            long l = b.getCreateTime();
            long alder = (nu-l)/1000/60/60/24;
            System.out.println("Alder: "+ alder + " dage - fra "+new Date(l));
            if (alder>5) b.delete();
        }
    }


    public static void main(String... args) throws Exception {
        new CloudStorageArbejdsfiler().oprydning();
    }
}
