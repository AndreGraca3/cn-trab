package pt.isel;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import pt.isel.firestore.LabelRepository;
import pt.isel.services.LabelService;

public class LabelApp {

    // GCP
    private static final String PROJECT_ID = "cn2324-t1-g15";

    // Pub/Sub
    private static final String LABELS_SUB_ID = "labels-sub";

    // Firestore
    private static final String COLLECTION_NAME = "Labels";
    private static final String DATABASE_ID = "cn-geral-db";

    public static void main(String[] args) {
        try {
            TopicAdminClient topicAdminClient = TopicAdminClient.create();
            PubSubOperations pubSubOperations = new PubSubOperations(topicAdminClient, PROJECT_ID);

            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(DATABASE_ID).setCredentials(credentials)
                    .build();
            Firestore db = options.getService();
            LabelRepository labelRepository = new LabelRepository(db);

            LabelService labelService = new LabelService(pubSubOperations, labelRepository);
            labelService.awaitImageRequest(LABELS_SUB_ID, COLLECTION_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}