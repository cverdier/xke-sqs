package fr.xebia.sqsjavabasic;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

import static com.amazonaws.regions.Regions.EU_WEST_2;
import static java.util.stream.Collectors.toList;

public class Application {

    public static void main(String[] args) throws Exception {

        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(EU_WEST_2)
                .build();

        String queueUrl = "";

        List<Message> messages = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages();
        messages.forEach(message -> {
            System.out.println("Message (" + message.getMessageId() + ")");
            System.out.println("    receipt-handle : " + message.getReceiptHandle());
            System.out.println("    content : " + message.getBody());
        });

        List<DeleteMessageBatchRequestEntry> deletions = messages.stream().map(message ->
                new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle())
        ).collect(toList());
        sqs.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, deletions));

        sqs.shutdown();
    }
}
