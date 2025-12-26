#!/bin/bash

echo "Iniciando config de LocalStack"

awslocal s3 mb s3://task-bucket
echo "Bucket 'task-bucket' creado."

awslocal sqs create-queue --queue-name task-queue
echo "Cola 'task-queue' creada."

awslocal ses verify-email-identity --email-address noreply@example.com
echo "Identidad de correo electrónico verificada en SES." 
echo "Configuración de LocalStack completada."
