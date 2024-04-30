package grpcclientapp.streams;

import forum.ExistingTopics;
import io.grpc.stub.StreamObserver;

public class ExistingTopicsStream implements StreamObserver<ExistingTopics> {

    public boolean isCompleted = false;

    @Override
    public void onNext(ExistingTopics existingTopics) {
        System.out.println(existingTopics);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Error sending message " + throwable.getMessage());
        isCompleted = true;
    }

    @Override
    public void onCompleted() {
        isCompleted = true;
    }
}
