package pt.isel;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PubSubOperations {

    TopicAdminClient topicAdminClient;

    public PubSubOperations(TopicAdminClient topicAdminClient) {
        this.topicAdminClient = topicAdminClient;
    }

    public void createTopic(String topicId) {
        topicAdminClient.createTopic(topicId);
    }

    public void deleteTopic(String topicId) {
        topicAdminClient.deleteTopic(topicId);
    }

    public void createSubscription(String subscriptionId, String topicId) throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            PushConfig pushConfig = PushConfig.getDefaultInstance();
            Subscription subscription =
                    subscriptionAdminClient
                            .createSubscription(subscriptionId, topicId, pushConfig, 0);
        }
    }

    public void deleteSubscription(String subscriptionId) {
        topicAdminClient.deleteTopic(subscriptionId);
    }

    public String publishMessageToTopic(String topicId, String message, Map<String, String> attributes) throws IOException, ExecutionException, InterruptedException {
        Publisher publisher = Publisher.newBuilder(topicId).build();
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(message))
                .putAllAttributes(attributes)
                .build();
        ApiFuture<String> future = publisher.publish(pubsubMessage);
        String msgID = future.get();
        publisher.shutdown();
        return msgID;
    }

    public void subscribeToTopic(String subscriptionId, MessageReceiver messageReceiver) {
        Subscriber subscriber = Subscriber.newBuilder(subscriptionId, messageReceiver).build();
        subscriber.startAsync().awaitRunning();
    }
}
