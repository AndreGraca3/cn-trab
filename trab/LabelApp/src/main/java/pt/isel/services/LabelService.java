package pt.isel.services;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import pt.isel.PubSubOperations;
import pt.isel.domain.Label;
import pt.isel.domain.LabeledImage;
import pt.isel.firestore.LabelRepository;
import pt.isel.translator.TranslatorService;
import pt.isel.vision.VisionService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class LabelService {

    private final PubSubOperations pubSubOperations;
    private final LabelRepository labelRepository;

    public LabelService(PubSubOperations pubSubOperations, LabelRepository labelRepository) {
        this.pubSubOperations = pubSubOperations;
        this.labelRepository = labelRepository;
    }

    public void awaitImageRequest(String labelsSubId, String saveCollectionName) {
        var subscriber = pubSubOperations.subscribeMessages(labelsSubId, new MessageReceiver() {
            @Override
            public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
                try {
                    // Process the message
                    var attributesMap = pubsubMessage.getAttributesMap();
                    var requestId = pubsubMessage.getData().toStringUtf8();
                    var bucketName = attributesMap.get("bucketName");
                    var blobName = attributesMap.get("blobName");

                    // Process the image
                    // Thread.sleep(9000); // Simulate processing time
                    var labels = createLabels(bucketName, blobName);
                    var image = new LabeledImage(requestId, blobName, labels);
                    labelRepository.saveLabeledImage(image, saveCollectionName); // Save image in Firestore

                    ackReplyConsumer.ack(); // Acknowledge the message only after processing
                    System.out.println("Labels saved for image: " + blobName);
                } catch (IOException | ExecutionException | InterruptedException e) {
                    ackReplyConsumer.nack();
                    e.printStackTrace();
                }
            }
        });
        subscriber.startAsync().awaitRunning();
        System.out.println("Awaiting images on subscription" + labelsSubId + "...");
        subscriber.awaitTerminated();
    }

    public List<Label> createLabels(String bucketName, String blobName) throws IOException {
        System.out.println("Processing image: " + blobName);
        var location = "gs://" + bucketName + "/" + blobName;
        var rawLabels = VisionService.detectLabels(location);

        return rawLabels.stream()
                .map(rawLabel -> new Label(rawLabel, TranslatorService.translate(rawLabel)))
                .collect(Collectors.toList());//.toList();
    }
}
