package pt.isel;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import label.*;
import pt.isel.streams.LabeledImageStream;
import pt.isel.streams.RequestIdStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static FunctionalServiceGrpc.FunctionalServiceBlockingStub blockingStub;
    private static FunctionalServiceGrpc.FunctionalServiceStub noBlockStub;

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
                    case 99:
                        System.exit(0);
                }
            } catch (Exception ex) {
                System.out.println("Execution call Error !");
                ex.printStackTrace();
            }
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
            System.out.println(" 5 - Insert operation here");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    static void isAlive() {
        var startTime = LocalDateTime.now();
        var ping = blockingStub.isAlive(
                RequestTimestamp
                        .newBuilder()
                        .setTimestamp(startTime.toString())
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
        var requestId = read("Insert request id: ", new Scanner(System.in));

        var labels = blockingStub.getLabeledImageByRequestId(
                RequestId.newBuilder().setId(requestId).build()
        );

        System.out.println("Labels for image " + requestId + ":");
        labels.getLabelsList().forEach(label ->
                System.out.println(label.getValue() + " - " + label.getTranslation())
        );
    }

    static void getLabeledImageByRequestIdAsync() {
        var requestId = read("Insert request id: ", new Scanner(System.in));

        noBlockStub.getLabeledImageByRequestId(
                RequestId.newBuilder().setId(requestId).build(), new LabeledImageStream()
        );
    }


    static void GetFileNamesWithLabel() {
        var label = read("Insert request label: ", new Scanner(System.in));

        var startdate = read("Insert request start date(yyyy-MM-dd): ", new Scanner(System.in));

        var enddate = read("Insert request end date(yyyy-MM-dd): ", new Scanner(System.in));



        var l = blockingStub.getFileNamesWithLabel(
                FileNamesWithLabelRequest.newBuilder().setLabel(label).setStartDate(startdate).setEndDate(enddate).build()
        );
        System.out.println("Labels for image " + label +", " + startdate+", " + enddate + ":");
        l.getFileNamesList().forEach(x ->
                System.out.println(x)
        );


    }



    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}