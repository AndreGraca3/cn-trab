package pt.isel.server.services;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import label.*;
import pt.isel.PubSubOperations;
import pt.isel.StorageOperations;
import pt.isel.firestore.LabelRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static pt.isel.utils.ArrayUtils.toByteArray;

public class FunctionalService extends FunctionalServiceGrpc.FunctionalServiceImplBase {

    private final StorageOperations storageOperations;
    private final PubSubOperations pubSubOperations;
    private final LabelRepository labelRepository;

    // Google Cloud Storage
    private final String BUCKET_NAME = "cn-2024-bucket-g15-eu";

    // Google Pub/Sub
    private static final String PENDING_LABEL_TOPIC_ID = "pending-labels";

    public FunctionalService(int svcPort, StorageOperations storageOperations, PubSubOperations pubSubOperations, LabelRepository labelRepository) {
        this.storageOperations = storageOperations;
        this.pubSubOperations = pubSubOperations;
        this.labelRepository = labelRepository;
        System.out.println("Functional Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(RequestTimestamp request, StreamObserver<PingResponse> responseObserver) {
        LocalDateTime startTime = LocalDateTime.parse(request.getTimestamp());
        var ping = (int) Duration.between(startTime, LocalDateTime.now()).toMillis();

        responseObserver.onNext(PingResponse.newBuilder().setPing(ping).build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ImageChunkRequest> submitImageForLabeling(StreamObserver<RequestId> responseObserver) {
        ArrayList<Byte> data = new ArrayList<>();
        return new StreamObserver<>() {

            @Override
            public void onNext(ImageChunkRequest chunk) {
                System.out.println("Received a block from client...");
                for (byte imageByte : chunk.getChunkData()) {
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
                    responseObserver.onNext(RequestId.newBuilder().setId(requestId).build());
                    responseObserver.onCompleted();
                } catch (IOException | ExecutionException | InterruptedException e) {
                    responseObserver.onError(e);
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void getLabeledImageByRequestId(RequestId request, StreamObserver<LabeledImageResponse> responseObserver) {
        try {

            // Retrieve labeled image from Firestore
            var image = labelRepository.getLabeledImage(request.getId(), "Labels");
            if (image == null) {
                responseObserver.onError(
                        Status.NOT_FOUND.withDescription("Image not found").asRuntimeException()
                );
                return;
            }

            var labelResponses = image.getLabels().stream()
                    .map(label ->
                            LabelResponse.newBuilder()
                                    .setValue(label.getValue())
                                    .setTranslation(label.getTranslation())
                                    .build()
                    )
                    .toList();

            responseObserver.onNext(LabeledImageResponse.newBuilder()
                    .setRequestId(RequestId.newBuilder().setId(image.getRequestId()).build())
                    .addAllLabels(labelResponses)
                    .build()
            );
            responseObserver.onCompleted();
        } catch (ExecutionException | InterruptedException e) {
            responseObserver.onError(e);
            e.printStackTrace();
        }
    }
}
