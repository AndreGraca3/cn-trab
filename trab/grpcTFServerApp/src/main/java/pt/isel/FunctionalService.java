package pt.isel;

import io.grpc.stub.StreamObserver;
import label.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FunctionalService extends ServiceGrpc.ServiceImplBase {

    private final StorageOperations storageOperations;
    private final PubSubOperations pubSubOperations;

    // Google Cloud Storage
    private final String BUCKET_NAME = "cn-2024-bucket-g15-europe";

    // Google Pub/Sub
    private static final String PENDING_LABEL_TOPIC_ID = "pending-labels";

    public FunctionalService(int svcPort, StorageOperations storageOperations, PubSubOperations pubSubOperations) {
        this.storageOperations = storageOperations;
        this.pubSubOperations = pubSubOperations;
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(RequestTimestamp request, StreamObserver<PingResult> responseObserver) {
        LocalDateTime startTime = LocalDateTime.parse(request.getTimestamp());
        var ping = (int) Duration.between(startTime, LocalDateTime.now()).toMillis();

        responseObserver.onNext(PingResult.newBuilder().setPing(ping).build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Block> uploadImage(StreamObserver<Identifier> responseObserver) {
        ArrayList<Byte> data = new ArrayList<>();
        return new StreamObserver<>() {

            @Override
            public void onNext(Block block) {
                System.out.println("Received a block from client...");
                for (byte imageByte : block.getBytes()) {
                    data.add(imageByte);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                String blobName = UUID.randomUUID().toString();

                try {
                    // Save image to Google Cloud Storage
                    byte[] imageBytes = toByteArray(data);
                    storageOperations.uploadBlobToBucket(BUCKET_NAME, blobName, imageBytes);

                    // Publish message to Pub/Sub
                    var requestId = BUCKET_NAME + blobName;
                    HashMap<String, String> attributes = new HashMap<>();
                    attributes.put("bucketName", BUCKET_NAME);
                    attributes.put("blobName", blobName);
                    pubSubOperations.publishMessageToTopic(PENDING_LABEL_TOPIC_ID, requestId, attributes);

                    // Send response to client
                    responseObserver.onNext(Identifier.newBuilder().setId(requestId).build());
                    responseObserver.onCompleted();
                } catch (IOException e) {
                    responseObserver.onError(e);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
