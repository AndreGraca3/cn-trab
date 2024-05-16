package pt.isel;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import pt.isel.translator.TranslatorService;
import pt.isel.vision.VisionService;

import java.io.IOException;

public class LabelService {

    private static final String PENDING_LABEL_TOPIC_ID = "projects/cn2324-t1-g15/topics/pending-labels";

    private final PubSubOperations pubSubOperations;
    private final FirebaseOperations firebaseOperations;

    public LabelService(PubSubOperations pubSubOperations, FirebaseOperations firebaseOperations) {
        this.pubSubOperations = pubSubOperations;
        this.firebaseOperations = firebaseOperations;
    }

    public void awaitImageProcessing() {
        pubSubOperations.subscribeToTopic(PENDING_LABEL_TOPIC_ID, new MessageReceiver() {
            @Override
            public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
                try {
                    // Process the message
                    var attributesMap = pubsubMessage.getAttributesMap();
                    var bucketName = attributesMap.get("bucketName");
                    var blobName = attributesMap.get("blobName");
                    createLabels(bucketName, blobName);
                    ackReplyConsumer.ack(); // Acknowledge the message only after processing
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void createLabels(String bucketName, String blobName) throws IOException {
        var location = "gs://" + bucketName + "/" + blobName;
        var rawLabels = VisionService.detectLabels(location);
        var translations = TranslatorService.translate(rawLabels);

        System.out.println("Labels: " + rawLabels);
        System.out.println("Translations: " + translations);
        // TODO: Save to Firestore
    }
}
