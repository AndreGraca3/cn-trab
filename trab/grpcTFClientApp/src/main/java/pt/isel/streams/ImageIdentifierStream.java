package pt.isel.streams;

import io.grpc.stub.StreamObserver;
import label.Identifier;

public class ImageIdentifierStream implements StreamObserver<Identifier> {

    public boolean isCompleted = false;

    @Override
    public void onNext(Identifier Id) {

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