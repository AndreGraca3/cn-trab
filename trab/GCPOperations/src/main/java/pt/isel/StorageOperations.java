package pt.isel;

import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageOperations {

    Storage storage;

    public StorageOperations(Storage storage) {
        this.storage = storage;
    }

    public void createBucket(String bucketName, String location) {
        storage.create(
                BucketInfo.newBuilder(bucketName)
                        // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
                        .setStorageClass(StorageClass.STANDARD)
                        // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
                        .setLocation(location)
                        .build());
    }

    public void deleteBucket(String bucketName) {
        Bucket bucket = storage.get(bucketName);
        bucket.delete();
    }

    /**
     * Uploads a file to a bucket.
     * @param bucketName - name of the bucket
     * @param blobName - name of the blob
     * @param uploadFrom - path to the file to upload
     * @throws IOException - if an error occurs while reading the file
     */
    public void uploadBlobToBucket(String bucketName, String blobName, Path uploadFrom) throws IOException {
        BlobId blobId = BlobId.of(bucketName, blobName);
        String contentType = Files.probeContentType(uploadFrom);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        if (Files.size(uploadFrom) > 1_000_000) {
            try (WriteChannel writer = storage.writer(blobInfo)) {
                byte[] buffer = new byte[1024];
                try (InputStream input = Files.newInputStream(uploadFrom)) {
                    int limit;
                    while ((limit = input.read(buffer)) >= 0) {
                        try {
                            writer.write(ByteBuffer.wrap(buffer, 0, limit));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } else {
            byte[] bytes = Files.readAllBytes(uploadFrom);
            storage.create(blobInfo, bytes);
        }
        System.out.println("Blob " + blobName + " created in bucket " + bucketName);
    }

    /**
     * Uploads a byte array to a bucket.
     * @param bucketName - name of the bucket
     * @param blobName - name of the blob
     * @param bytes - byte array to upload
     */
    public void uploadBlobToBucket(String bucketName, String blobName, String contentType, byte[] bytes) throws IOException {
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        // if buffer size is greater than 1MB, use WriteChannel
        if (bytes.length > 1_000_000) {
            try (WriteChannel writer = storage.writer(blobInfo)) {
                writer.write(ByteBuffer.wrap(bytes));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            storage.create(blobInfo, bytes);
        }
        System.out.println("Blob " + blobName + " created in bucket " + bucketName);
    }

    public void downloadBlobFromBucket(String bucketName, String blobName, String outFolderName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("No such Blob exists !");
            return;
        }
        createFolder(outFolderName);
        Path filepath = Path.of(outFolderName + blobName);
        PrintStream writeTo = new PrintStream(Files.newOutputStream(filepath));
        if (blob.getSize() < 1_000_000) {
            // Blob is small read all its content in one request
            byte[] content = blob.getContent();
            writeTo.write(content);
        } else {
            // When Blob size is big or unknown use the blob's channel reader.
            try (ReadChannel reader = blob.reader()) {
                WritableByteChannel channel = Channels.newChannel(writeTo);
                ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                while (reader.read(bytes) > 0) {
                    bytes.flip();
                    channel.write(bytes);
                    bytes.clear();
                }
            }
        }
        writeTo.close();
        System.out.println("Blob " + blobName + " downloaded to " + filepath);
    }

    public void updateBlobAccessControl(String bucketName, String blobName, Acl.Role role) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("No such Blob exists!");
            return;
        }

        Acl.Entity blobUsers = Acl.User.ofAllUsers();

        Acl acl = Acl.newBuilder(blobUsers, role).build();
        blob.createAcl(acl);
    }

    public void deleteBlob(String bucketName, String blobName) {
        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("No such Blob exists!");
            return;
        }
        blob.delete();
    }

    private void createFolder(String path) {
        File fileDirectory = new File(path);
        if (!fileDirectory.exists()) fileDirectory.mkdirs();
    }
}
