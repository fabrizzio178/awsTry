package com.aws.worker_service.service;

import com.aws.worker_service.domain.Task;
import com.aws.worker_service.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskProcessorTest {

    // 1. Mockeamos todas las dependencias externas
    @Mock private SqsClient sqsClient;
    @Mock private S3Client s3Client;
    @Mock private TaskRepository taskRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private PdfService pdfService;
    @Mock private SesClient sesClient;

    // 2. Inyectamos los mocks en tu servicio real
    @InjectMocks
    private TaskProcessor taskProcessor;

    @BeforeEach
    void setup() {
        // las variables @Value son null. Las seteamos "a la fuerza" con Reflection.
        ReflectionTestUtils.setField(taskProcessor, "queueName", "test-queue");
        ReflectionTestUtils.setField(taskProcessor, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("Debería procesar un mensaje correctamente: Generar PDF, Subir S3, Enviar Email y Borrar de SQS")
    void processQueue_FlujoExitoso() throws Exception {
        // GIVEN
        String queueUrl = "https://sqs.aws.com/test-queue";
        Long taskId = 100L;
        String messageBody = "{\"taskId\": 100}";

        // Simular obtener URL de la cola
        GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queueUrl).build();
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(urlResponse);

        // Simular que SQS devuelve 1 mensaje
        Message message = Message.builder().body(messageBody).receiptHandle("handle-123").build();
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder().messages(message).build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveResponse);

        // Simular el ObjectMapper leyendo el JSON
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(messageBody)).thenReturn(jsonNode);
        when(jsonNode.get("taskId")).thenReturn(jsonNode); 
        when(jsonNode.asLong()).thenReturn(taskId);

        // Simular que la Tarea existe en DB
        Task task = new Task();
        task.setId(taskId);
        task.setDescription("Tarea de prueba");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        // Simular generación de PDF
        byte[] pdfFake = new byte[]{1, 2, 3};
        when(pdfService.generarReportePdf(taskId, "Tarea de prueba")).thenReturn(pdfFake);

        // WHEN
        taskProcessor.processQueue();

        // THEN
        // Verificamos subida a S3
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Verificamos envio de mail SES
        verify(sesClient).sendEmail(any(SendEmailRequest.class));

        // Verificamos actualizacion en DB 
        verify(taskRepository).save(argThat(t -> 
            t.getStatus().equals("COMPLETED") && 
            t.getResultUrl().contains("report_task_100.pdf")
        ));

        // Verificamos que el mensaje se borro de SQS 
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("No debería hacer nada si la cola SQS está vacía")
    void processQueue_ColaVacia() {
        // GIVEN
        String queueUrl = "https://sqs.aws.com/test-queue";
        GetQueueUrlResponse urlResponse = GetQueueUrlResponse.builder().queueUrl(queueUrl).build();
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(urlResponse);

        // SQS devuelve lista vacía
        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder().messages(Collections.emptyList()).build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveResponse);

        // WHEN
        taskProcessor.processQueue();

        // THEN
        // Verificamos que NO se llamó a nada más
        verify(taskRepository, never()).findById(anyLong());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}