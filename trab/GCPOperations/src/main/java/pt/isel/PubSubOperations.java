package pt.isel;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PubSubOperations {

    TopicAdminClient topicAdminClient;
    private final String projectId;

    public PubSubOperations(TopicAdminClient topicAdminClient, String projectId) {
        this.topicAdminClient = topicAdminClient;
        this.projectId = projectId;
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
        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = Publisher.newBuilder(topicName).build();
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(message))
                .putAllAttributes(attributes)
                .build();
        ApiFuture<String> future = publisher.publish(pubsubMessage);
        String msgID = future.get();
        publisher.shutdown();
        return msgID;
    }

    public Subscriber subscribeMessages(
            String subscriptionId,
            MessageReceiver messageReceiver
    ) {
        ProjectSubscriptionName projSubscriptionName =
                ProjectSubscriptionName.of(projectId, subscriptionId);
        ExecutorProvider executorProvider = InstantiatingExecutorProvider
                .newBuilder()
                .setExecutorThreadCount(1)
                .build();
        return Subscriber.newBuilder(projSubscriptionName, messageReceiver)
                .setExecutorProvider(executorProvider)
                .build();
    }
}
