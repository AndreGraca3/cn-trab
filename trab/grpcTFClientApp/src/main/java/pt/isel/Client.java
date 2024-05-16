package pt.isel;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import label.Block;
import label.RequestTimestamp;
import label.ServiceGrpc;
import pt.isel.streams.ImageIdentifierStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ServiceGrpc.ServiceBlockingStub blockingStub;
    private static ServiceGrpc.ServiceStub noBlockStub;

    private static final int BLOCK_CAPACITY = 64 * 1024; // 64 KB

    public static void main(String[] args) {
        try {
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
            blockingStub = ServiceGrpc.newBlockingStub(channel);
            noBlockStub = ServiceGrpc.newStub(channel);
            // Call service operations for example ping server
            while (true) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            isAlive();
                            break;
                        case 2:
                            uploadImageAsynchronousCall();
                            break;
                        case 99:
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error !");
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }


    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Case Unary call: server isAlive");
            System.out.println(" 2 - Upload an image");
            System.out.println(" 3 - Insert operation here");
            System.out.println(" 4 - Insert operation here");
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
        System.out.println("Ping is " + ping.getPing());
    }

    public static byte[] getImageBytes(String imagePath) throws IOException {
        File file = new File(imagePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] imageBytes = new byte[(int) file.length()];
        fis.read(imageBytes);
        fis.close();
        return imageBytes;
    }

    static void uploadImageAsynchronousCall() throws IOException {
        var scanner = new Scanner(System.in);
        String file = read("Insert path to file: ", scanner);
        ImageIdentifierStream imageIdentifierStream = new ImageIdentifierStream();
        StreamObserver<Block> blockStreamObserver = noBlockStub.uploadImage(imageIdentifierStream);
        ByteBuffer byteBuffer = ByteBuffer.allocate(BLOCK_CAPACITY);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.read(byteBuffer.array()) > 0) {
                byteBuffer.flip();
                blockStreamObserver.onNext(
                        Block.newBuilder().setBytes(ByteString.copyFrom(byteBuffer.array())).build()
                );
                byteBuffer.clear();
            }
            blockStreamObserver.onCompleted();
        }
        while (!imageIdentifierStream.isCompleted) {
            System.out.println("currently waiting for Identifier...");
        }
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}