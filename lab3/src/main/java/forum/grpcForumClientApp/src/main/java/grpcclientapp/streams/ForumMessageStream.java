package grpcclientapp.streams;

import forum.ForumMessage;
import io.grpc.stub.StreamObserver;
import storageoperations.StorageOperations;

import java.io.IOException;

public class ForumMessageStream implements StreamObserver<ForumMessage> {

    private StorageOperations storageOperations;

    public ForumMessageStream(StorageOperations storageOperations) {
        this.storageOperations = storageOperations;
    }

    public boolean isCompleted = false;

    @Override
    public void onNext(ForumMessage forumMessage) {
        System.out.println("[::" + forumMessage.getTopicName() + "::]" + "Message sent by " + forumMessage.getFromUser() + ": " + forumMessage.getTxtMsg());
        // crazy stuff
        var msg = forumMessage.getTxtMsg();
        var parts = msg.split("\\[;");

        if(parts.length < 2) return; // no blob, no download
        var blobInfoParts = parts[1].substring(0, parts[1].length() - 1).split(";");
        var bucketName = blobInfoParts[0];
        var blobName = blobInfoParts[1];
        try {
            storageOperations.downloadBlobFromBucket(bucketName, blobName);
        } catch (IOException e) {
            System.out.println("Error downloading blob ");
            e.printStackTrace();
        }
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