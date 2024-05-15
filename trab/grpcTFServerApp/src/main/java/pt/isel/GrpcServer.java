package pt.isel;

// generic ServerApp for hosting grpcService

import com.google.cloud.storage.StorageOptions;
import io.grpc.ServerBuilder;
import pt.isel.storage.StorageOperations;

public class GrpcServer {

    private static int svcPort = 8000;

    public static void main(String[] args) {
        try {
            StorageOperations storageOperations = new StorageOperations(StorageOptions.getDefaultInstance().getService());
            if (args.length > 0) svcPort = Integer.parseInt(args[0]);
            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    // Add one or more services.
                    // The Server can host many services in same TCP/IP port
                    .addService(new Service(svcPort, storageOperations))
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