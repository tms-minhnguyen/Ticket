package com.example.ticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

/**
 * AWS SQS configuration using LocalStack for local development.
 */
@Configuration
public class SqsConfig {

    @Value("${aws.sqs.endpoint:http://localhost:4566}")
    private String sqsEndpoint;

    @Value("${aws.sqs.region:ap-southeast-1}")
    private String region;

    @Value("${aws.sqs.access-key:localstack}")
    private String accessKey;

    @Value("${aws.sqs.secret-key:localstack}")
    private String secretKey;

    /**
     * SQS async client configured for LocalStack.
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(sqsEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
