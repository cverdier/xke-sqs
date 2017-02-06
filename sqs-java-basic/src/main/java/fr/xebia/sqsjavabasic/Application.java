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

        // SQS Client
        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(EU_WEST_2)
                .build();

        // The SQS queue we're working with
        String queueUrl = "xke-test";

        // ReceiveMessage request
        System.out.println("Making request to queue '" + queueUrl + "'");
        List<Message> messages = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages();
        System.out.println("Request returned " + messages.size() + " messages");

        // Logging the received messages
        messages.forEach(message -> {
            System.out.println("Message (" + message.getMessageId() + ")");
            System.out.println("    receipt-handle : " + message.getReceiptHandle());
            System.out.println("    content : " + message.getBody());
        });

        // Preparing the ack : BatchDeleteRequest
        List<DeleteMessageBatchRequestEntry> deletions = messages.stream().map(message ->
                new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle())
        ).collect(toList());

        // Delete the messages, if we received any
        if (! messages.isEmpty()) {
            System.out.println("Deleting " + messages.size() + " messages from '" + queueUrl + "'");
            DeleteMessageBatchResult result = sqs.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, deletions));
            System.out.println("Deletion result : " + result.toString());
        }

        // Shutdown the SQS client
        sqs.shutdown();
    }
}
