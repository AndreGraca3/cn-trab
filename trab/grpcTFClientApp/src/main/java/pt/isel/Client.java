package pt.isel;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import label.*;
import management.InstanceCount;
import management.ManagementServiceGrpc;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import pt.isel.domain.InstancesList;
import pt.isel.streams.RequestIdStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Client {
    private static final String cFunctionUrl = "https://europe-west2-cn2324-t1-g15.cloudfunctions.net/cf-list-group-run-vms";

    private static int svcPort = 7500;
    private static FunctionalServiceGrpc.FunctionalServiceBlockingStub functionalServiceBlockingStub;
    private static FunctionalServiceGrpc.FunctionalServiceStub functionalServiceStub;

    private static ManagementServiceGrpc.ManagementServiceBlockingStub managementServiceBlockingStub;
    private static ManagementServiceGrpc.ManagementServiceStub managementServiceStub;

    private static final int BLOCK_CAPACITY = 64 * 1024; // 64 KB

    public static void main(String[] args) {
        String svcIP = "localhost";

        if (args.length == 1) {
            svcPort = Integer.parseInt(args[0]);
        }

        // Choose IP from the list of IPs
        System.out.println("Searching for available instances...");

        try(CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet reqGet = new HttpGet(cFunctionUrl);
            CloseableHttpResponse respGet = httpclient.execute(reqGet);
            HttpEntity entity = respGet.getEntity();
            String jstr = EntityUtils.toString(entity);

            InstancesList instancesList = new Gson().fromJson(jstr, InstancesList.class);
            var ips = instancesList.getInstanceIps();

            switch (ips.length) {
                case 0:
                    System.out.println("No instances available");
                    System.exit(0);
                    break;
                case 1:
                    svcIP = ips[0];
                    break;
                default:
                    System.out.println("Choose an IP from the list of IPs: ");
                    for (int i = 0; i < ips.length; i++) {
                        System.out.println(i + " - " + instancesList.getInstanceIps()[i]);
                    }

                    String option = read("Select IP: ", new Scanner(System.in));
                    svcIP = instancesList.getInstanceIps()[Integer.parseInt(option)];
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("connect to " + svcIP + ":" + svcPort);
        // Channels are secure by default (via SSL/TLS).
        // For the example we disable TLS to avoid
        // needing certificates.
        ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                // Channels are secure by default (via SSL/TLS).
                // For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
        functionalServiceBlockingStub = FunctionalServiceGrpc.newBlockingStub(channel);
        functionalServiceStub = FunctionalServiceGrpc.newStub(channel);

        managementServiceBlockingStub = ManagementServiceGrpc.newBlockingStub(channel);
        managementServiceStub = ManagementServiceGrpc.newStub(channel);

        while (true) {
            try {
                int option = Menu();
                switch (option) {
                    case 1:
                        isAlive();
                        break;
                    case 2:
                        submitImageForLabeling();
                        break;
                    case 3:
                        getLabeledImageByRequestId();
                        break;
                    case 4:
                        GetFileNamesWithLabel();
                        break;
                    case 5:
                        resizeGRPCServerInstances();
                        break;
                    case 6:
                        resizeLabelAppInstances();
                        break;
                    case 99:
                        System.exit(0);
                }
            } catch (Exception ex) {
                System.out.println("Execution call Error !");
                ex.printStackTrace();
            }
            System.out.println();
            read("Press Enter to continue...", new Scanner(System.in));
            System.out.println("-".repeat(50));
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Ping server");
            System.out.println(" 2 - Submit an image for labeling");
            System.out.println(" 3 - Get labels for an image");
            System.out.println(" 4 - Search images by labels and date");
            System.out.println(" 5 - Resize gRPC server instances");
            System.out.println(" 6 - Resize gRPC label instances");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 6) || op == 99));
        return op;
    }

    static void isAlive() {
        var startTime = System.currentTimeMillis();
        functionalServiceBlockingStub.isAlive(Empty.newBuilder().build());
        System.out.println("Ping is " + (System.currentTimeMillis() - startTime) + " ms");
    }

    static void submitImageForLabeling() throws IOException {
        var scanner = new Scanner(System.in);
        String file = read("Insert path to file: ", scanner);
        Path path = Path.of(file);

        // Call the service operation to get the stream to send the image
        RequestIdStream requestIdStream = new RequestIdStream();
        StreamObserver<ImageChunkRequest> imageChunkStreamObserver
                = functionalServiceStub.submitImageForLabeling(requestIdStream);

        // Send the image in blocks
        ByteBuffer byteBuffer = ByteBuffer.allocate(BLOCK_CAPACITY);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.read(byteBuffer.array()) > 0) {
                byteBuffer.flip();
                var imageChunkRequest = ImageChunkRequest.newBuilder()
                        .setChunkData(ByteString.copyFrom(byteBuffer.array()))
                        .setFileName(path.getFileName().toString())
                        .setContentType(Files.probeContentType(path))
                        .build();
                imageChunkStreamObserver.onNext(imageChunkRequest);
                byteBuffer.clear();
            }
            imageChunkStreamObserver.onCompleted();
        }
    }

    static void getLabeledImageByRequestId() {
        var scanner = new Scanner(System.in);
        var requestId = read("Insert request id: ", scanner);

        var labels = functionalServiceBlockingStub.getLabeledImageByRequestId(
                RequestId.newBuilder().setId(requestId).build()
        );

        System.out.println("Labels for image " + requestId + ":");
        labels.getLabelsList().forEach(label ->
                System.out.println(label.getValue() + " - " + label.getTranslation())
        );
    }

    static void GetFileNamesWithLabel() {
        var scanner = new Scanner(System.in);
        var label = read("Insert request label: ", scanner);
        var startDate = read("Insert request start date(yyyy-MM-dd): ", scanner);
        var endDate = read("Insert request end date(yyyy-MM-dd): ", scanner);

        var fileNames = functionalServiceBlockingStub.getFileNamesWithLabel(
                FileNamesWithLabelRequest
                        .newBuilder()
                        .setLabel(label)
                        .setStartDate(startDate)
                        .setEndDate(endDate)
                        .build()
        );
        System.out.println("File names for label " + label + " between " + startDate + " and " + endDate + ":");
        fileNames.getFileNamesList().forEach(System.out::println);
    }

    // Management Service

    static void resizeGRPCServerInstances() {
        var scanner = new Scanner(System.in);
        var count = read("Insert the number of instances to change to: ", scanner);

        InstanceCount instanceCount = InstanceCount.newBuilder().setCount(Integer.parseInt(count)).build();
        managementServiceStub.changeGRPCServerInstances(instanceCount, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error changing the number of gRPC server instances");
            }

            @Override
            public void onCompleted() {
                System.out.println("Changed the number of gRPC server instances to " + count + " successfully");
            }
        });

        System.out.println("Server instances number change scheduled, please wait...");
    }

    static void resizeLabelAppInstances() {
        var scanner = new Scanner(System.in);
        var count = read("Insert the number of instances to change to: ", scanner);

        InstanceCount instanceCount = InstanceCount.newBuilder().setCount(Integer.parseInt(count)).build();
        managementServiceStub.changeImageProcessingInstances(instanceCount, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error changing the number of Label App instances");
            }

            @Override
            public void onCompleted() {
                System.out.println("Changed the number of Label App instances to " + count + " successfully");
            }
        });

        System.out.println("Label App instances number change scheduled, please wait...");
    }

    // helper method to read a string from the console
    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}