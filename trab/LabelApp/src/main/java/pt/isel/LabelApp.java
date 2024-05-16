package pt.isel;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;

public class LabelApp {

    private static final String collectionName = "Labels";
    private static final String databaseId = "cn-geral-db";

    public static void main(String[] args) {
        try {
            TopicAdminClient topicAdminClient = TopicAdminClient.create();
            PubSubOperations pubSubOperations = new PubSubOperations(topicAdminClient);

            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(databaseId).setCredentials(credentials)
                    .build();
            Firestore db = options.getService();
            FirebaseOperations firebaseOperations = new FirebaseOperations(db, collectionName);

            LabelService labelService = new LabelService(pubSubOperations, firebaseOperations);
            labelService.awaitImageProcessing();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}