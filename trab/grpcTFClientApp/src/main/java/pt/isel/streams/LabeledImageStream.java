package pt.isel.streams;

import io.grpc.stub.StreamObserver;
import label.LabeledImageResponse;

public class LabeledImageStream implements StreamObserver<LabeledImageResponse> {

    public boolean isCompleted = false;

    @Override
    public void onNext(LabeledImageResponse labeledImageResponse) {
        System.out.println("Labels for image " + labeledImageResponse.getRequestId().getId() + ":");
        labeledImageResponse.getLabelsList().forEach(label ->
                System.out.println(label.getValue() + " - " + label.getTranslation())
        );
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
        isCompleted = true;
    }

    @Override
    public void onCompleted() {
        isCompleted = true;
    }
}
