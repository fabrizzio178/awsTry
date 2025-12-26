package com.aws.api_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aws.api_service.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long>{}
