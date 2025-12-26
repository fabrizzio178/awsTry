package com.aws.worker_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aws.worker_service.domain.Task;
public interface TaskRepository extends JpaRepository<Task, Long>{}
