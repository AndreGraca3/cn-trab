package pt.isel.streams;

import io.grpc.stub.StreamObserver;
import label.RequestId;

public class RequestIdStream implements StreamObserver<RequestId> {

    public boolean isCompleted = false;

    @Override
    public void onNext(RequestId id) {
        System.out.println("Request id: " + id.getId());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println(throwable.getMessage());
        isCompleted = true;
    }

    @Override
    public void onCompleted() {
        isCompleted = true;
    }
}