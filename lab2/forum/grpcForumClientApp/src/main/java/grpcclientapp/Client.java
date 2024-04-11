package grpcclientapp;

import grpcclientapp.streams.SendMessageStream;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import forum.*;

import java.util.*;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ServiceGrpc.ServiceBlockingStub blockingStub;
    private static ServiceGrpc.ServiceStub noBlockStub;

    private static String username;

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }
            System.out.println("connect to " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    // Channels are secure by default (via SSL/TLS).
                    // For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build();
            blockingStub = ServiceGrpc.newBlockingStub(channel);
            noBlockStub = ServiceGrpc.newStub(channel);
            // Call service operations for example ping server
            boolean end = false;
            username = read("Enter your username: ", new Scanner(System.in));
            while (!end) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            isAliveCall();
                            break;
                        case 2:
                            publishMessageSynchronousCall();
                            break;
                        case 3:
                            publishMessageAsynchronousCall();
                            break;
                        case 99:
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
            read("prima enter to end", new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    static void isAliveCall() {
        //ping server
        TextMessage reply = blockingStub.isAlive(ProtoVoid.newBuilder().build());
        System.out.println("Ping server:" + reply.getTxt());
    }

    static void publishMessageSynchronousCall() {
        //Synchronous blocking call
        try {
            String message = read("Message: ", new Scanner(System.in));
            String topic = read("Topic: ", new Scanner(System.in));
            ForumMessage forumMessage = ForumMessage
                    .newBuilder()
                    .setTopicName(topic)
                    .setFromUser(username)
                    .setTxtMsg(message)
                    .build();
            blockingStub.publishMessage(forumMessage);
        } catch (Exception ex) {
            System.out.println("Synchronous call error: " + ex.getMessage());
        }
    }

    static void publishMessageAsynchronousCall() throws InterruptedException {
        // Asynchronous non-blocking call
        String message = read("Message:", new Scanner(System.in));
        String topic = read("Topic:", new Scanner(System.in));
        ForumMessage forumMessage = ForumMessage
                .newBuilder()
                .setTopicName("topic")
                .setFromUser(username)
                .setTxtMsg(message)
                .build();
        SendMessageStream sendMessageStream = new SendMessageStream();
        noBlockStub.publishMessage(forumMessage, sendMessageStream);
        while (!sendMessageStream.isCompleted) {
            System.out.println("Continue working until the message is sent");
            Thread.sleep(1000); // Simulate processing time (1 seg)
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Case Unary call: server isAlive");
            System.out.println(" 2 - Case Unary call: publish message: Synchronous call");
            System.out.println(" 3 - Case Unary call: publish message: Asynchronous call");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}
