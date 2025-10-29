// STEP_36 Service層実装例

package com.example.taskapp.service;

import com.example.taskapp.entity.Task;
import com.example.taskapp.entity.enums.Priority;
import com.example.taskapp.entity.enums.TaskStatus;
import com.example.taskapp.dto.TaskSearchCriteria;
import com.example.taskapp.dto.request.TaskCreateRequest;
import com.example.taskapp.mapper.TaskMapper;
import com.example.taskapp.mapper.ProjectMapper;
import com.example.taskapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    /**
     * タスク作成
     */
    @Transactional
    public Task createTask(TaskCreateRequest request) {
        log.info("タスクを作成します: {}", request.getTitle());
        
        // プロジェクトの存在確認
        projectMapper.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("プロジェクトが見つかりません"));
        
        Task task = new Task();
        task.setProjectId(request.getProjectId());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(Priority.valueOf(request.getPriority()));
        task.setAssigneeId(request.getAssigneeId());
        task.setDueDate(request.getDueDate());
        
        taskMapper.insert(task);
        log.info("タスクを作成しました: id={}", task.getId());
        
        return task;
    }

    /**
     * タスク取得
     */
    public Task getTaskById(Long id) {
        return taskMapper.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("タスクが見つかりません: id=" + id));
    }

    /**
     * プロジェクトのタスク一覧取得
     */
    public List<Task> getProjectTasks(Long projectId) {
        return taskMapper.findByProjectId(projectId);
    }

    /**
     * タスク検索
     */
    public List<Task> searchTasks(TaskSearchCriteria criteria) {
        log.info("タスクを検索します: {}", criteria);
        return taskMapper.search(criteria);
    }

    /**
     * タスク更新
     */
    @Transactional
    public Task updateTask(Long id, TaskCreateRequest request) {
        log.info("タスクを更新します: id={}", id);
        
        Task task = getTaskById(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.valueOf(request.getStatus()));
        task.setPriority(Priority.valueOf(request.getPriority()));
        task.setAssigneeId(request.getAssigneeId());
        task.setDueDate(request.getDueDate());
        
        taskMapper.update(task);
        log.info("タスクを更新しました: id={}", id);
        
        return task;
    }

    /**
     * ステータス更新
     */
    @Transactional
    public void updateTaskStatus(Long id, String status) {
        log.info("タスクステータスを更新します: id={}, status={}", id, status);
        
        // タスク存在確認
        getTaskById(id);
        
        taskMapper.updateStatus(id, status);
    }

    /**
     * タスク削除
     */
    @Transactional
    public void deleteTask(Long id) {
        log.info("タスクを削除します: id={}", id);
        
        // タスク存在確認
        getTaskById(id);
        
        taskMapper.deleteById(id);
    }

    /**
     * 期限切れタスク取得
     */
    public List<Task> getOverdueTasks(Long userId) {
        return taskMapper.findOverdueTasks(userId);
    }
}

// ========================================

package com.example.taskapp.service;

import com.example.taskapp.entity.Project;
import com.example.taskapp.dto.request.ProjectCreateRequest;
import com.example.taskapp.mapper.ProjectMapper;
import com.example.taskapp.mapper.UserMapper;
import com.example.taskapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    /**
     * プロジェクト作成
     */
    @Transactional
    public Project createProject(ProjectCreateRequest request, String username) {
        log.info("プロジェクトを作成します: {}", request.getName());
        
        // ユーザー取得
        var user = userMapper.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwnerId(user.getId());
        
        projectMapper.insert(project);
        log.info("プロジェクトを作成しました: id={}", project.getId());
        
        return project;
    }

    /**
     * プロジェクト取得
     */
    public Project getProjectById(Long id) {
        return projectMapper.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("プロジェクトが見つかりません: id=" + id));
    }

    /**
     * ユーザーのプロジェクト一覧取得
     */
    public List<Project> getUserProjects(String username) {
        var user = userMapper.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        
        return projectMapper.findByUserId(user.getId());
    }

    /**
     * プロジェクト更新
     */
    @Transactional
    public Project updateProject(Long id, ProjectCreateRequest request) {
        log.info("プロジェクトを更新します: id={}", id);
        
        Project project = getProjectById(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        
        projectMapper.update(project);
        log.info("プロジェクトを更新しました: id={}", id);
        
        return project;
    }

    /**
     * プロジェクト削除
     */
    @Transactional
    public void deleteProject(Long id) {
        log.info("プロジェクトを削除します: id={}", id);
        
        // プロジェクト存在確認
        getProjectById(id);
        
        projectMapper.deleteById(id);
    }
}

// ========================================

package com.example.taskapp.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
