package grpcserverapp;

import com.google.protobuf.Empty;
import forum.*;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Service extends ServiceGrpc.ServiceImplBase {

    // topic -> { map: username -> streamObserver }
    ConcurrentMap<String, ConcurrentMap<String, StreamObserver<ForumMessage>>> topicSubscribers;
    //ConcurrentMap<Topics, ConcurrentMap<Username, StreamObserver<ForumMessages>>>

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
        topicSubscribers = new ConcurrentHashMap<>();
    }

    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        System.out.println("Method isAlive called with params: protoVoid");
        responseObserver.onNext(TextMessage.newBuilder().setTxt("Service is alive").build());
        responseObserver.onCompleted();
    }

    @Override
    public void topicSubscribe(SubscribeUnSubscribe request, StreamObserver<ForumMessage> responseObserver) {
        String topic = request.getTopicName();
        String username = request.getUsrName();
        System.out.println("Method topicSubscribe called with params: \n" +
                "topic: " + topic + "\n" +
                "username: " + username
        );
        if (topic == null || username == null) {
            responseObserver.onError(
                    new StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid request parameters"))
            );
            return;
        }

        ServerCallStreamObserver<ForumMessage> scs0 = (ServerCallStreamObserver<ForumMessage>) responseObserver;
        scs0.setOnCancelHandler(() -> topicSubscribers.get(topic).remove(username).onCompleted());

        topicSubscribers.computeIfAbsent(topic, k -> new ConcurrentHashMap<>()).put(username, scs0);

        ForumMessage topicSubscribeMessage = ForumMessage.newBuilder().setFromUser(username).setTopicName(topic).setTxtMsg("Subscribed").build();
        topicSubscribers.get(topic).forEach((user_name, observer) -> observer.onNext(topicSubscribeMessage));
        scs0.onNext(topicSubscribeMessage);
    }

    @Override
    public void topicUnSubscribe(SubscribeUnSubscribe request, StreamObserver<Empty> responseObserver) {
        String topic = request.getTopicName();
        String username = request.getUsrName();
        System.out.println("Method topicUnSubscribe called with params: \n" +
                "topic: " + topic + "\n" +
                "username: " + username
        );
        if (topic == null || username == null) {
            responseObserver.onError(
                    new StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid request parameters"))
            );
            return;
        }
        ConcurrentMap<String, StreamObserver<ForumMessage>> subscribers = topicSubscribers.get(topic);
        if (subscribers == null) {
            responseObserver.onError(
                    new StatusException(Status.NOT_FOUND.withDescription("Topic not found"))
            );
            return;
        }
        subscribers.remove(username).onCompleted();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTopics(Empty request, StreamObserver<ExistingTopics> responseObserver) {
        System.out.println("Method getAllTopics called with params: Empty");
        ExistingTopics.Builder builder = ExistingTopics.newBuilder();
        topicSubscribers.forEach(
                (key, value) -> builder.addTopicName(key)
        );
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void publishMessage(ForumMessage request, StreamObserver<Empty> responseObserver) {
        String topic = request.getTopicName();
        System.out.println("Method publishMessage called with params:\n" + "topic: " + topic);
        if (topic == null) {
            responseObserver.onError(
                    new StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid request parameters"))
            );
            return;
        }
        ConcurrentMap<String, StreamObserver<ForumMessage>> subscribers = topicSubscribers.get(topic);
        if (subscribers == null) {
            responseObserver.onError(
                    new StatusException(Status.NOT_FOUND.withDescription("No subscribers for topic"))
            );
            return;
        }
        subscribers.forEach((user_name, observer) -> {
            try {
                observer.onNext(request);
            } catch (CancellationException e) {
                topicSubscribers.get(topic).remove(user_name).onCompleted();
            }
        });
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
