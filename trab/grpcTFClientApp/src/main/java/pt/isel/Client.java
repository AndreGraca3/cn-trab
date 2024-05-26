package pt.isel;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import label.*;
import management.InstanceCount;
import management.ManagementServiceGrpc;
import pt.isel.streams.LabeledImageStream;
import pt.isel.streams.RequestIdStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    private static String svcIP = "localhost";
    private static int svcPort = 7500;
    private static FunctionalServiceGrpc.FunctionalServiceBlockingStub blockingStub;
    private static FunctionalServiceGrpc.FunctionalServiceStub noBlockStub;

    private static ManagementServiceGrpc.ManagementServiceBlockingStub blockingStubManagement;
    private static ManagementServiceGrpc.ManagementServiceStub noBlockStubManagement;

    private static final int BLOCK_CAPACITY = 64 * 1024; // 64 KB

    public static void main(String[] args) {
        if (args.length == 2) {
            svcIP = args[0];
            svcPort = Integer.parseInt(args[1]);
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
        blockingStub = FunctionalServiceGrpc.newBlockingStub(channel);
        noBlockStub = FunctionalServiceGrpc.newStub(channel);
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
                        ChangeGRPCServerInstances();
                        break;
                    case 6:
                        ChangeGRPCLabelInstances();
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
            System.out.println(" 1 - Case Unary call: server isAlive");
            System.out.println(" 2 - Submit an image for labeling");
            System.out.println(" 3 - Get labels for an image");
            System.out.println(" 4 - Search images by labels and date");
            System.out.println(" 5 - Change gRPC server instances");
            System.out.println(" 6 - Change gRPC label instances");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 4) || op == 99));
        return op;
    }

    static void isAlive() {
        var ping = blockingStub.isAlive(
                PingRequest
                        .newBuilder()
                        .setRequestTimeMillis(System.nanoTime())
                        .build());
        System.out.println("Ping is " + ping.getPing() + "ms");
    }

    static void submitImageForLabeling() throws IOException {
        var scanner = new Scanner(System.in);
        String file = read("Insert path to file: ", scanner);

        // Call the service operation to get the stream to send the image
        RequestIdStream requestIdStream = new RequestIdStream();
        StreamObserver<ImageChunkRequest> imageChunkStreamObserver
                = noBlockStub.submitImageForLabeling(requestIdStream);

        // Send the image in blocks
        ByteBuffer byteBuffer = ByteBuffer.allocate(BLOCK_CAPACITY);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.read(byteBuffer.array()) > 0) {
                byteBuffer.flip();
                var imageChunkRequest = ImageChunkRequest.newBuilder()
                        .setChunkData(ByteString.copyFrom(byteBuffer.array()))
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

        var labels = blockingStub.getLabeledImageByRequestId(
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

        var fileNames = blockingStub.getFileNamesWithLabel(
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

    static void ChangeGRPCServerInstances() {
        var scanner = new Scanner(System.in);
        var count = read("Insert the number of instances to increase: ", scanner);

        InstanceCount instanceCount = InstanceCount.newBuilder().setCount(Integer.parseInt(count)).build();
        noBlockStubManagement.changeGRPCServerInstances(instanceCount, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                System.out.println("Increased the number of gRPC server instances by " + count);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error increasing the number of gRPC server instances");
            }

            @Override
            public void onCompleted() {
                System.out.println("Increased the number of gRPC server instances by " + count);
            }
        });

        System.out.println("Increased the number of gRPC server instances by " + count);
    }

    static void ChangeGRPCLabelInstances() {
        var scanner = new Scanner(System.in);
        var count = read("Insert the number of instances to increase: ", scanner);

        InstanceCount instanceCount = InstanceCount.newBuilder().setCount(Integer.parseInt(count)).build();
        noBlockStubManagement.changeImageProcessingInstances(instanceCount, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                System.out.println("Increased the number of gRPC server instances by " + count);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error increasing the number of gRPC server instances");
            }

            @Override
            public void onCompleted() {
                System.out.println("Increased the number of gRPC server instances by " + count);
            }
        });

        System.out.println("Increased the number of Label instances by " + count);
    }

    // helper method to read a string from the console
    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}