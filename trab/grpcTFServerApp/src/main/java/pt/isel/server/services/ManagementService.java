package pt.isel.server.services;

import io.grpc.stub.StreamObserver;
import management.ManagementServiceGrpc;
import management.PingResponse;
import management.RequestTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;

public class ManagementService extends ManagementServiceGrpc.ManagementServiceImplBase {

    @Override
    public void isAlive(RequestTimestamp request, StreamObserver<PingResponse> responseObserver) {
        LocalDateTime startTime = LocalDateTime.parse(request.getTimestamp());
        var ping = (int) Duration.between(startTime, LocalDateTime.now()).toMillis();

        responseObserver.onNext(PingResponse.newBuilder().setPing(ping).build());
        responseObserver.onCompleted();
    }
}
