package com.aws.api_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@SpringBootApplication
public class ApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServiceApplication.class, args);
	}

	@Bean
    public CommandLineRunner testAwsConnection(S3Client s3Client, SqsClient sqsClient) {
        return args -> {
            System.out.println("--- PRUEBA DE CONEXIÓN A AWS LOCALSTACK ---");

            try {
                // Listar Buckets
                System.out.println("Buckets encontrados:");
                s3Client.listBuckets().buckets().forEach(b -> System.out.println(" - " + b.name()));
            } catch (Exception ex) {
                System.err.println("Fallo listBuckets() contra Localstack (no se aborta el arranque): " + ex.getMessage());
                ex.printStackTrace(System.err);
            }

            try {
                // Listar Colas
                System.out.println("Colas SQS encontradas:");
                sqsClient.listQueues().queueUrls().forEach(q -> System.out.println(" - " + q));
            } catch (Exception ex) {
                System.err.println("Fallo listQueues() contra Localstack (no se aborta el arranque): " + ex.getMessage());
                ex.printStackTrace(System.err);
            }

            System.out.println("-------------------------------------------");
        };
    }

}
