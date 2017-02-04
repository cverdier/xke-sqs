package fr.xebia.sqsjavabasic;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.List;

import static com.amazonaws.regions.Regions.EU_WEST_2;
import static java.util.stream.Collectors.toList;

public class Application {

    public static void main(String[] args) throws Exception {

        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(EU_WEST_2)
                .build();

        String queueUrl = "xke-test";

        System.out.println("Making request to queue '" + queueUrl + "'");
        List<Message> messages = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages();
        System.out.println("Request returned " + messages.size() + " messages");
        messages.forEach(message -> {
            System.out.println("Message (" + message.getMessageId() + ")");
            System.out.println("    receipt-handle : " + message.getReceiptHandle());
            System.out.println("    content : " + message.getBody());
        });

        List<DeleteMessageBatchRequestEntry> deletions = messages.stream().map(message ->
                new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle())
        ).collect(toList());
        if (! messages.isEmpty()) {
            System.out.println("Deleting " + messages.size() + " messages from '" + queueUrl + "'");
            DeleteMessageBatchResult result = sqs.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, deletions));
            System.out.println("Deletion result : " + result.toString());
        }

        sqs.shutdown();
    }
}
