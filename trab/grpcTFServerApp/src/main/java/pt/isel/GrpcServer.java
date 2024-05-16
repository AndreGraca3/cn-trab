package pt.isel;

// generic ServerApp for hosting grpcService

import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.storage.StorageOptions;
import io.grpc.ServerBuilder;

public class GrpcServer {

    private static int svcPort = 8000;

    private static final String PROJECT_ID = "cn2324-t1-g15";

    public static void main(String[] args) {
        try {
            StorageOperations storageOperations =
                    new StorageOperations(StorageOptions.getDefaultInstance().getService());

            TopicAdminClient topicAdminClient = TopicAdminClient.create();
            PubSubOperations pubSubOperations = new PubSubOperations(topicAdminClient, PROJECT_ID);

            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    .addService(new FunctionalService(svcPort, storageOperations, pubSubOperations))
                    // Add elasticity management service here on same port
                    .build();
            svc.start();
            System.out.println("Server started on port " + svcPort);
            // Java virtual machine shutdown hook
            // to capture normal or abnormal exits
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));
            // Waits for the server to become terminated
            svc.awaitTermination();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}