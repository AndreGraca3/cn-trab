package pt.isel.server;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.storage.StorageOptions;
import io.grpc.ServerBuilder;
import pt.isel.PubSubOperations;
import pt.isel.StorageOperations;
import pt.isel.firestore.LabelRepository;
import pt.isel.server.services.FunctionalService;
import pt.isel.server.services.ManagementService;

public class GrpcServer {

    private static int svcPort = 7500;

    // GCP
    private static final String PROJECT_ID = "cn2324-t1-g15";

    // Firestore
    private static final String DATABASE_ID = "cn-geral-db";

    public static void main(String[] args) {
        try {
            StorageOperations storageOperations =
                    new StorageOperations(StorageOptions.getDefaultInstance().getService());

            TopicAdminClient topicAdminClient = TopicAdminClient.create();
            PubSubOperations pubSubOperations = new PubSubOperations(topicAdminClient, PROJECT_ID);

            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            FirestoreOptions options = FirestoreOptions
                    .newBuilder().setDatabaseId(DATABASE_ID).setCredentials(credentials)
                    .build();
            Firestore db = options.getService();
            LabelRepository labelRepository = new LabelRepository(db);

            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    .addService(new FunctionalService(svcPort, storageOperations, pubSubOperations, labelRepository))
                    .addService(new ManagementService(svcPort))
                    .build();

            svc.start();
            System.out.println("Server started on port " + svcPort);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));
            svc.awaitTermination();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}