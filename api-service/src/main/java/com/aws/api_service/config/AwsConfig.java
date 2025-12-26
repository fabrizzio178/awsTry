package com.aws.api_service.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Data
@Configuration
public class AwsConfig {
    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    @Value("${aws.sqs.endpoint}")
    private String sqsEndpoint;

    private final StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("test", "test"));
    
    @Bean
    public S3Client s3Client(){
		System.out.println("AWS S3 endpoint (raw): " + s3Endpoint);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build())
                .endpointOverride(URI.create(s3Endpoint))
                .build();
    }

    @Bean
    public SqsClient sqsClient(){
		System.out.println("AWS SQS endpoint (raw): " + sqsEndpoint);
        return SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(sqsEndpoint))
                .build();
    }


}