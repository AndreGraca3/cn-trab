package grpcclientapp.streams;

import forum.ForumMessage;
import io.grpc.stub.StreamObserver;

public class ForumMessageStream implements StreamObserver<ForumMessage> {

    public boolean isCompleted = false;

    @Override
    public void onNext(ForumMessage forumMessage) {
        System.out.println("[::" + forumMessage.getTopicName() + "::]" + "Message sent by " + forumMessage.getFromUser() + ": " + forumMessage.getTxtMsg());
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