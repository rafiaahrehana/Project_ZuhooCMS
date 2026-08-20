package com.businessos.modules.servicedesk.task;

import java.util.List;

public interface TaskService {
    TaskResponse addTask(Long requestId, CreateTaskRequest request);
    List<TaskResponse> getTasks(Long requestId);
    TaskResponse updateTask(Long requestId, Long taskId, UpdateTaskRequest request);
    void deleteTask(Long requestId, Long taskId);
}
