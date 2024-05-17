package pt.isel;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.pubsub.v1.PubsubMessage;
import pt.isel.domain.Log;
import pt.isel.firestore.LoggingRepository;

import java.util.concurrent.ExecutionException;

public class LoggingService {

    PubSubOperations pubSubOperations;
    LoggingRepository firestoreService;

    public LoggingService(PubSubOperations pubSubOperations, LoggingRepository firestoreService) {
        this.pubSubOperations = pubSubOperations;
        this.firestoreService = firestoreService;
    }

    /**
     * Awaits for image requests and saving the logs to Firestore
     * @param logsSubId - The subscription ID to listen for logs
     * @param collectionName - The firestore collection name to save the logs
     */
    public void awaitImageRequest(String logsSubId, String collectionName) {
        var subscriber = pubSubOperations.subscribeMessages(logsSubId, new MessageReceiver() {
            @Override
            public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer ackReplyConsumer) {
                try {
                    // Process the log
                    var attributesMap = pubsubMessage.getAttributesMap();
                    var bucketName = attributesMap.get("bucketName");
                    var blobName = attributesMap.get("blobName");
                    var logId = pubsubMessage.getData().toStringUtf8();

                    var log = new Log(logId, bucketName, blobName);
                    firestoreService.saveLog(log, collectionName);
                    ackReplyConsumer.ack(); // Acknowledge the message only after processing
                    System.out.println("Log processed: " + logId);
                } catch (ExecutionException | InterruptedException e) {
                    ackReplyConsumer.nack();
                    e.printStackTrace();
                }
            }
        });
        subscriber.startAsync().awaitRunning();
        System.out.println("Awaiting logs on " + logsSubId + "...");
        subscriber.awaitTerminated();
    }
}
