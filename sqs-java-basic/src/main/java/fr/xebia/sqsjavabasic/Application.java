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

        // The SQS queue we're working with
        String queueUrl = "xke-java";

        // ReceiveMessage request

        // Logging the received messages

        // Preparing the ack : BatchDeleteRequest

        // Delete the messages, if we received any

        // Shutdown the SQS client
    }
}
