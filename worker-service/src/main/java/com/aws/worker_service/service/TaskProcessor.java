package com.aws.worker_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aws.worker_service.domain.Task;
import com.aws.worker_service.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskProcessor {
    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final PdfService pdfService;
    private final SesClient sesClient;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        try {
            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
            
            // polling
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build()
            );

            List<Message> messages = response.messages();
            if (!messages.isEmpty()) {
                log.info("Mensajes recibidos: {}", messages.size());
                messages.forEach(message -> procesarMensaje(queueUrl, message));
            }

        } catch (Exception e) {
            log.error("Error al procesar la cola SQS", e);
        }
    }

    private void procesarMensaje(String queueUrl, Message message) {
        try {
            String body = message.body();
            JsonNode jsonNode = objectMapper.readTree(body);
            Long taskId = jsonNode.get("taskId").asLong();

            log.info("Procesando tarea con ID: {}", taskId);

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Tarea no encontrada: " + taskId));

            // generar y subir pdf
            log.info("Generando reporte PDF para la tarea ID: {}", taskId);
            byte[] pdfBytes = pdfService.generarReportePdf(taskId, task.getDescription());
            String pdfFileName = "report_task_" + taskId + ".pdf";

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(pdfFileName)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(pdfBytes));

            String fileUrl = "http://localhost:4566/" + bucketName + "/" + pdfFileName;
            log.info("Reporte PDF subido a S3: {}", fileUrl);

            // enviar mail
            enviarEmailNotificacion("fabrizziosana10@gmail.com", taskId, fileUrl);

            // actualizamos la db
            task.setStatus("COMPLETED");
            task.setResultUrl(fileUrl);
            taskRepository.save(task);

            // borra mensaje de la cola
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build()
            );
            log.info("Tarea procesada con éxito y mensaje eliminado de SQS: {}", taskId);

        } catch (Exception e) {
            log.error("Error al procesar el mensaje (Se reintentará)", e);
        }
    }

    private void enviarEmailNotificacion(String destinatario, Long taskId, String fileUrl) {
        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(destinatario).build())
                .message(software.amazon.awssdk.services.ses.model.Message.builder()
                        .subject(Content.builder().data("Tarea " + taskId + " Finalizada - Reporte PDF").build())
                        .body(Body.builder()
                                .text(Content.builder().data("Hola Fabrizzio, su reporte está listo.\nDescárguelo aquí: " + fileUrl).build())
                                .build())
                        .build())
                .source("noreply@example.com") 
                .build();
        
        sesClient.sendEmail(request);
        log.info("Email enviado simuladamente a {}", destinatario);
    }
}