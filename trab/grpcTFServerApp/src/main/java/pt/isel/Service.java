package pt.isel;

import io.grpc.stub.StreamObserver;
import label.Block;
import label.Identifier;
import label.ServiceGrpc;
import pt.isel.storage.StorageOperations;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;

public class Service extends ServiceGrpc.ServiceImplBase {

    private int _svcPort;
    private StorageOperations _storageOperations;

    private final String BUCKET_NAME = "cn-2024-bucket-g15-asia";
    private final String pathString = "./grpcTFServerApp/downloads/";

    public Service(int svcPort, StorageOperations storageOperations) {
        _svcPort = svcPort;
        _storageOperations = storageOperations;
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public StreamObserver<Block> uploadImage(StreamObserver<Identifier> responseObserver) {
        return new StreamObserver<>() {
            ArrayList<Byte> data = new ArrayList<>();

            @Override
            public void onNext(Block block) {
                System.out.println("Received a block...");
                for (byte imageByte : block.getBytes()) {
                    data.add(imageByte);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {
                String blobName = "ola.png";
                try {
                    byte[] imageBytes = toByteArray(data);
                    String filePathString = pathString + blobName;
                    Path filePath = Path.of(filePathString);
                    FileOutputStream fos = new FileOutputStream(filePathString);
                    fos.write(imageBytes);
                    _storageOperations.uploadBlobToBucket(BUCKET_NAME, blobName, filePath);
                } catch (IOException e) {
                    responseObserver.onError(e);
                }
                responseObserver.onNext(Identifier.newBuilder().setId(BUCKET_NAME + blobName).build());
                responseObserver.onCompleted();
            }
        };
    }

    private byte[] toByteArray(ArrayList<Byte> data) {
        byte[] result = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i);
        }
        return result;
    }

}
