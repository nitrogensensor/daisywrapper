package eu.nitrogensensor.daisylib.remote.google_cloud_storage;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GemOgHent {
    static Storage getStorage() throws IOException {

        try {
            String jsonPath = "/home/j/Projekter/NitrogenSensor/gitlab/nitrogensensor/daisy/daisykørsel-arbejdsfiler.json";
            // You can specify a credential file by providing a path to GoogleCredentials.
            // Otherwise credentials are read from the GOOGLE_APPLICATION_CREDENTIALS environment variable.
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            return storage;
        } catch (FileNotFoundException e) {
        }
        Storage storage = StorageOptions.getDefaultInstance().getService();
        return storage;

    }



    public static void main(String... args) throws Exception {

        // Instantiates a client
        Storage storage = getStorage();
        Page<Bucket> buckets = storage.list();
        for (Bucket bucket : buckets.iterateAll()) {
            System.out.println(bucket.toString());
        }

        Bucket bucket = storage.get("daisykoersel-arbejdsfiler");

        for (Blob b : bucket.list(Storage.BlobListOption.currentDirectory()).iterateAll()) {
            System.out.println(b.toString());
        }

        Blob b = bucket.get("README.md");
        b.downloadTo(Paths.get("/tmp/xxx"));


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

        skrivZip();
    }

    private static void skrivZip() throws IOException {
        Map<String, String> env = new HashMap<>();
// Create the zip file if it doesn't exist
        env.put("create", "true");

        URI uri = URI.create("jar:file:/tmp/zipfstest.zip");

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get("README.md");
            Path pathInZipfile = zipfs.getPath("README.md");
            // Copy a file into the zip file
            Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
        }

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            Path externalTxtFile = Paths.get("README.md");
            Path pathInZipfile = zipfs.getPath("README.md");
            // Copy a file into the zip file
            Files.copy(externalTxtFile, pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
        }

    }
}
