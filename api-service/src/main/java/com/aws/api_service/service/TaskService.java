package com.aws.api_service.service;

import org.springframework.stereotype.Service;

import com.aws.api_service.DTO.TaskRequest;
import com.aws.api_service.model.Task;
import com.aws.api_service.repository.TaskRepository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@Data
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-name}")
    private String queueName;

    public Task createTask(TaskRequest request){
        Task task = new Task();
        task.setDescription(request.getDescription());
        Task tareaGuardada = taskRepository.save(task);

        log.info("Tarea guardada en la base de datos: {}", tareaGuardada);
        try{
            // Obtiene el url de la cola
            String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();

            // Creamos el mensaje json
            // Estructura: {"taskId":1,"description":"Mi tarea"}
            Map<String, Long> messageBody = new HashMap<>();
            messageBody.put("taskId", tareaGuardada.getId());
            String jsonMessage = objectMapper.writeValueAsString(messageBody);

            // Enviar el mensaje a la cola SQS
            sqsClient.sendMessage(SendMessageRequest.builder()  
                .queueUrl(queueUrl)
                .messageBody(jsonMessage)
                .build()
            );
            System.out.println("Mensaje enviado a la cola SQS: " + jsonMessage);
            log.info("Mensaje enviado a la cola SQS: {}", jsonMessage);
        } catch (Exception e){
            log.error("Error al enviar mensaje a SQS", e);
            throw new RuntimeException("Fallo al encolar la tarea");
        }
        return tareaGuardada;
    }



}
