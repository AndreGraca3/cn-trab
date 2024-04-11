package grpcserverapp;

import com.google.protobuf.Empty;
import forum.*;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Service extends ServiceGrpc.ServiceImplBase {

    // topic -> { map: username -> streamObserver }
    ConcurrentMap<String, ConcurrentMap<String, StreamObserver<ForumMessage>>> topicSubscribers;

    public Service(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        responseObserver.onNext(TextMessage.newBuilder().setTxt("Service is alive").build());
        responseObserver.onCompleted();
    }

    @Override
    public void topicSubscribe(SubscribeUnSubscribe request, StreamObserver<ForumMessage> responseObserver) {
        String topic = request.getTopicName();
        String username = request.getUsrName();
        if (topic == null || username == null) {
            responseObserver.onError(
                    new StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid request parameters"))
            );
            return;
        }
        topicSubscribers.computeIfAbsent(topic, k -> new ConcurrentHashMap<>())
                .put(username, responseObserver);
    }

    @Override
    public void publishMessage(ForumMessage request, StreamObserver<Empty> responseObserver) {
        String topic = request.getTopicName();
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
        subscribers.forEach((username, observer) -> {
            observer.onNext(request);
        });
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
