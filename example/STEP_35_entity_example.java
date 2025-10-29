// STEP_35 エンティティ実装例

// ========================================
// Enum定義
// ========================================

package com.example.taskapp.entity.enums;

public enum TaskStatus {
    TODO("未着手"),
    IN_PROGRESS("進行中"),
    DONE("完了");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

// ========================================

package com.example.taskapp.entity.enums;

public enum Priority {
    LOW("低", "#6c757d"),
    MEDIUM("中", "#ffc107"),
    HIGH("高", "#dc3545");
    
    private final String displayName;
    private final String color;
    
    Priority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColor() {
        return color;
    }
}

// ========================================

package com.example.taskapp.entity.enums;

public enum ProjectRole {
    OWNER("オーナー"),
    MEMBER("メンバー");
    
    private final String displayName;
    
    ProjectRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

// ========================================
// エンティティ定義
// ========================================

package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// ========================================

package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Project {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // MyBatis JOIN結果用（データベースには存在しないフィールド）
    private String ownerName;
}

// ========================================

package com.example.taskapp.entity;

import com.example.taskapp.entity.enums.Priority;
import com.example.taskapp.entity.enums.TaskStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Task {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private Long assigneeId;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // MyBatis JOIN結果用
    private String projectName;
    private String assigneeName;
}

// ========================================

package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // JOIN結果用
    private String username;
    private String userName;
}

// ========================================

package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createdAt;
}

// ========================================

package com.example.taskapp.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskTag {
    private Long taskId;
    private Long tagId;
    private LocalDateTime createdAt;
}

// ========================================

package com.example.taskapp.entity;

import com.example.taskapp.entity.enums.ProjectRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectMember {
    private Long id;
    private Long projectId;
    private Long userId;
    private ProjectRole role;
    private LocalDateTime joinedAt;
    
    // JOIN結果用
    private String username;
    private String userName;
}
