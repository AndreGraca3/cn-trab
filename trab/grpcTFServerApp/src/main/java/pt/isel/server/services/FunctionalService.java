package pt.isel.server.services;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import label.*;
import pt.isel.PubSubOperations;
import pt.isel.StorageOperations;
import pt.isel.domain.LabeledImage;
import pt.isel.firestore.LabelRepository;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static pt.isel.utils.ArrayUtils.toByteArray;

public class FunctionalService extends FunctionalServiceGrpc.FunctionalServiceImplBase {

    private final StorageOperations storageOperations;
    private final PubSubOperations pubSubOperations;
    private final LabelRepository labelRepository;

    // Google Cloud Storage
    private final String BUCKET_NAME = "cn-2024-bucket-g15-eu";

    // Google Pub/Sub
    private static final String PENDING_LABEL_TOPIC_ID = "pending-labels";

    // Firestore
    private static final String LABELS_COLLECTION_NAME = "Labels";

    public FunctionalService(int svcPort, StorageOperations storageOperations, PubSubOperations pubSubOperations, LabelRepository labelRepository) {
        this.storageOperations = storageOperations;
        this.pubSubOperations = pubSubOperations;
        this.labelRepository = labelRepository;
        System.out.println("Functional Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(Empty request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ImageChunkRequest> submitImageForLabeling(StreamObserver<RequestId> responseObserver) {
        ArrayList<Byte> data = new ArrayList<>();

        return new StreamObserver<>() {
            String blobName;
            String contentType;

            @Override
            public void onNext(ImageChunkRequest chunkInfo) {
                System.out.println("Received a block from client...");
                blobName = chunkInfo.getFileName();
                contentType = chunkInfo.getContentType();

                for (byte imageByte : chunkInfo.getChunkData()) {
                    data.add(imageByte);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println(throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                try {
                    // Save image to Google Cloud Storage
                    byte[] imageBytes = toByteArray(data);
                    storageOperations.uploadBlobToBucket(BUCKET_NAME, blobName, contentType, imageBytes);

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
            var image = labelRepository.getLabeledImage(request.getId(), LABELS_COLLECTION_NAME);
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
                    .collect(Collectors.toList()); //.toList();


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

    @Override
    public void getFileNamesWithLabel(FileNamesWithLabelRequest request, StreamObserver<FileNamesWithLabelResponse> responseObserver) {
        try {
            var simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            var labels = labelRepository.getLabeledImagesByLabel(
                    request.getLabel(),
                    simpleDateFormat.parse(request.getStartDate()),
                    simpleDateFormat.parse(request.getEndDate()),
                    LABELS_COLLECTION_NAME
            );
            var fileNames = labels.stream().map(LabeledImage::getFileName).collect(Collectors.toList());//.toList();

            responseObserver.onNext(FileNamesWithLabelResponse.newBuilder()
                    .addAllFileNames(fileNames)
                    .build()
            );
            responseObserver.onCompleted();

        } catch (ExecutionException | InterruptedException | ParseException e) {
            responseObserver.onError(e);
            e.printStackTrace();
        }
    }
}
