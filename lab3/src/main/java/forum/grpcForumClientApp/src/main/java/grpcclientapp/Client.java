package grpcclientapp;

import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.Empty;
import forum.*;
import grpcclientapp.streams.ExistingTopicsStream;
import grpcclientapp.streams.ForumMessageStream;
import grpcclientapp.streams.SendMessageStream;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import storageoperations.StorageOperations;

import java.util.Scanner;

public class Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "localhost"; //"35.209.47.109";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static ServiceGrpc.ServiceBlockingStub blockingStub;
    private static ServiceGrpc.ServiceStub noBlockStub;
    private static StorageOperations storageOperations
            = new StorageOperations(StorageOptions.getDefaultInstance().getService());

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
                            topicSubscribeSynchronousCall();
                            break;
                        case 3:
                            topicSubscribeAsynchronousCall();
                            break;
                        case 4:
                            break;
                        case 5:
                            topicUnSubscribeAsynchronousCall();
                            break;
                        case 6:
                            getAllTopicsSynchronousCall();
                            break;
                        case 7:
                            getAllTopicsAsynchronousCall();
                            break;
                        case 8:
                            publishMessageSynchronousCall();
                            break;
                        case 9:
                            publishMessageAsynchronousCall();
                            break;
                        case 99:
                            System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error !");
                    ex.printStackTrace();
                }
            }
            read("Press enter to end", new Scanner(System.in));
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

    static void topicSubscribeSynchronousCall() {
        String topic = read("Topic:", new Scanner(System.in));
        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder().setUsrName(username).setTopicName(topic).build();
        blockingStub.topicSubscribe(request);
    }

    static void topicSubscribeAsynchronousCall() {
        String topic = read("Topic:", new Scanner(System.in));

        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder().setUsrName(username).setTopicName(topic).build();
        ForumMessageStream messageStream = new ForumMessageStream(storageOperations);
        noBlockStub.topicSubscribe(request, messageStream);
    }

    static void topicUnSubscribeAsynchronousCall() {
        // Prompt user for topic
        String topic = read("Enter the topic to unsubscribe from: ", new Scanner(System.in));

        // Create a request message
        SubscribeUnSubscribe request = SubscribeUnSubscribe.newBuilder().setUsrName(username).setTopicName(topic).build();
        noBlockStub.topicUnSubscribe(request, new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
                System.out.println("Unsubscribed successfully");
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Call completed");
            }
        });
    }

    static void getAllTopicsSynchronousCall() {
        try {
            System.out.println(blockingStub.getAllTopics(Empty.newBuilder().build()));
        } catch (Exception ex) {
            System.out.println("Synchronous call error: " + ex.getMessage());
        }
    }

    static void getAllTopicsAsynchronousCall() {
        ExistingTopicsStream existingTopicsStream = new ExistingTopicsStream();
        noBlockStub.getAllTopics(Empty.newBuilder().build(), existingTopicsStream);
        while (!existingTopicsStream.isCompleted) {
            System.out.println("Continue working until the message is sent");
            //Thread.sleep(1000); // Simulate processing time (1 seg)
        }
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
        var scanner = new Scanner(System.in);
        String message = read("Message:", scanner);
        String topic = read("Topic:", scanner);
        ForumMessage forumMessage = ForumMessage
                .newBuilder()
                .setTopicName(topic)
                .setFromUser(username)
                .setTxtMsg(message)
                .build();
        SendMessageStream sendMessageStream = new SendMessageStream();
        noBlockStub.publishMessage(forumMessage, sendMessageStream);
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Case Unary call: server isAlive"); // done
            System.out.println(" 2 - Case Unary call: topic subscribe: Synchronous call");
            System.out.println(" 3 - Case Unary call: topic subscribe: Asynchronous call");
            System.out.println(" 4 - Case Unary call: topic unsubscribe: Synchronous call");
            System.out.println(" 5 - Case Unary call: topic unsubscribe: Asynchronous call");
            System.out.println(" 6 - Case Unary call: all topics: Synchronous call"); // done
            System.out.println(" 7 - Case Unary call: all topics: Asynchronous call"); // done
            System.out.println(" 8 - Case Unary call: publish message: Synchronous call");
            System.out.println(" 9 - Case Unary call: publish message: Asynchronous call");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 9) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }
}
