package com.example.ticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for AWS SQS message queue operations.
 * Uses LocalStack for local development.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.endpoint:http://localhost:4566}")
    private String sqsEndpoint;

    @Value("${aws.sqs.queue-name:payment-queue}")
    private String queueName;

    private String queueUrl;

    @PostConstruct
    public void init() {
        this.queueUrl = sqsEndpoint + "/000000000000/" + queueName;
        log.info("SQS Queue URL: {}", queueUrl);

        // Ensure queue exists
        createQueueIfNotExists();
    }

    /**
     * Create queue if it doesn't exist (for LocalStack).
     */
    private void createQueueIfNotExists() {
        try {
            sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build()).join();
            log.info("SQS queue created/verified: {}", queueName);
        } catch (Exception e) {
            log.warn("Could not create queue (may already exist): {}", e.getMessage());
        }
    }

    /**
     * Send a message to the payment queue.
     */
    public CompletableFuture<SendMessageResponse> sendOrderPaidMessage(String orderCode) {
        try {
            Map<String, Object> messageBody = Map.of(
                    "type", "ORDER_PAID",
                    "orderCode", orderCode,
                    "timestamp", System.currentTimeMillis());

            String messageJson = objectMapper.writeValueAsString(messageBody);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageJson)
                    .build();

            return sqsAsyncClient.sendMessage(request)
                    .thenApply(response -> {
                        log.info("Sent ORDER_PAID message for order {}: messageId={}",
                                orderCode, response.messageId());
                        return response;
                    });
        } catch (Exception e) {
            log.error("Failed to send SQS message for order {}", orderCode, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Receive messages from the payment queue.
     */
    public CompletableFuture<List<Message>> receiveMessages(int maxMessages) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(5) // Long polling
                .visibilityTimeout(30)
                .build();

        return sqsAsyncClient.receiveMessage(request)
                .thenApply(ReceiveMessageResponse::messages);
    }

    /**
     * Delete a message from the queue after processing.
     */
    public CompletableFuture<DeleteMessageResponse> deleteMessage(String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();

        return sqsAsyncClient.deleteMessage(request)
                .thenApply(response -> {
                    log.debug("Deleted message with receipt: {}", receiptHandle);
                    return response;
                });
    }

    /**
     * Parse message body to extract order code.
     */
    public String extractOrderCode(String messageBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(messageBody, Map.class);
            return (String) body.get("orderCode");
        } catch (Exception e) {
            log.error("Failed to parse message body: {}", messageBody, e);
            return null;
        }
    }
}
