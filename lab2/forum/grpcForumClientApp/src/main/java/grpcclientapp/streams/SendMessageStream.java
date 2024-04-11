package grpcclientapp.streams;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class SendMessageStream implements StreamObserver<Empty> {

    public boolean isCompleted = false;

    @Override
    public void onNext(Empty empty) {
        System.out.println("Message sent");
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
