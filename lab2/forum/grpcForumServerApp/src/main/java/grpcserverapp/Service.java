package grpcserverapp;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import forum.ServiceGrpc;
import java.util.Random;

import static com.google.common.math.IntMath.isPrime;

public class Service extends ServiceGrpc.ServiceImplBase {
    public Service(int svcPort) {

        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        System.out.println("isAlive called!");
        responseObserver.onNext(TextMessage.newBuilder().setTxt("Service is alive").build());
        responseObserver.onCompleted();
    }
}
