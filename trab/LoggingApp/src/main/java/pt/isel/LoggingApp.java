package pt.isel;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import pt.isel.firestore.LoggingRepository;

import java.io.IOException;

public class LoggingApp {

    // GCP
    private static final String PROJECT_ID = "cn2324-t1-g15";

    // Firestore
    private static final String DATABASE_ID = "cn-geral-db";

    // PubSub
    private static final String LOGS_SUB_ID = "logs-sub";
    private static final String COLLECTION_NAME = "Logs";

    public static void main(String[] args) {
        TopicAdminClient topicAdminClient;
        try {
            topicAdminClient = TopicAdminClient.create();
            PubSubOperations pubSubOperations = new PubSubOperations(topicAdminClient, PROJECT_ID);

            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(DATABASE_ID).setCredentials(credentials)
                    .build();
            Firestore db = options.getService();

            LoggingRepository firestoreRepository = new LoggingRepository(db);

            LoggingService loggingService = new LoggingService(pubSubOperations, firestoreRepository);
            loggingService.awaitImageRequest(LOGS_SUB_ID, COLLECTION_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}