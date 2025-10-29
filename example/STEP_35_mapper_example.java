// STEP_35 MyBatis Mapper実装例

package com.example.taskapp.mapper;

import com.example.taskapp.entity.Task;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TaskMapper {
    
    /**
     * タスク作成
     */
    @Insert("""
        INSERT INTO tasks (project_id, title, description, status, priority, assignee_id, due_date)
        VALUES (#{projectId}, #{title}, #{description}, #{status}, #{priority}, #{assigneeId}, #{dueDate})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Task task);
    
    /**
     * ID検索
     */
    @Select("""
        SELECT * FROM tasks WHERE id = #{id}
    """)
    Optional<Task> findById(Long id);
    
    /**
     * ID検索（詳細情報付き）
     */
    @Select("""
        SELECT 
            t.*,
            p.name as project_name,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN projects p ON t.project_id = p.id
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE t.id = #{id}
    """)
    Optional<Task> findByIdWithDetails(Long id);
    
    /**
     * プロジェクトのタスク一覧
     */
    @Select("""
        SELECT 
            t.*,
            u.username as assignee_name
        FROM tasks t
        LEFT JOIN users u ON t.assignee_id = u.id
        WHERE t.project_id = #{projectId}
        ORDER BY 
            CASE t.status
                WHEN 'TODO' THEN 1
                WHEN 'IN_PROGRESS' THEN 2
                WHEN 'DONE' THEN 3
            END,
            t.created_at DESC
    """)
    List<Task> findByProjectId(Long projectId);
    
    /**
     * タスク更新
     */
    @Update("""
        UPDATE tasks
        SET title = #{title},
            description = #{description},
            status = #{status},
            priority = #{priority},
            assignee_id = #{assigneeId},
            due_date = #{dueDate},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void update(Task task);
    
    /**
     * ステータス更新
     */
    @Update("""
        UPDATE tasks
        SET status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * タスク削除
     */
    @Delete("DELETE FROM tasks WHERE id = #{id}")
    void deleteById(Long id);
}

// ========================================

package com.example.taskapp.mapper;

import com.example.taskapp.entity.Project;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMapper {
    
    @Insert("""
        INSERT INTO projects (name, description, owner_id)
        VALUES (#{name}, #{description}, #{ownerId})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Project project);
    
    @Select("SELECT * FROM projects WHERE id = #{id}")
    Optional<Project> findById(Long id);
    
    @Select("""
        SELECT 
            p.*,
            u.username as owner_name
        FROM projects p
        LEFT JOIN users u ON p.owner_id = u.id
        WHERE p.id = #{id}
    """)
    Optional<Project> findByIdWithDetails(Long id);
    
    @Select("""
        SELECT DISTINCT p.*, u.username as owner_name
        FROM projects p
        LEFT JOIN users u ON p.owner_id = u.id
        LEFT JOIN project_members pm ON p.id = pm.project_id
        WHERE p.owner_id = #{userId} OR pm.user_id = #{userId}
        ORDER BY p.created_at DESC
    """)
    List<Project> findByUserId(Long userId);
    
    @Update("""
        UPDATE projects
        SET name = #{name},
            description = #{description},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void update(Project project);
    
    @Delete("DELETE FROM projects WHERE id = #{id}")
    void deleteById(Long id);
}

// ========================================

package com.example.taskapp.mapper;

import com.example.taskapp.entity.Comment;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CommentMapper {
    
    @Insert("""
        INSERT INTO comments (task_id, user_id, content)
        VALUES (#{taskId}, #{userId}, #{content})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Comment comment);
    
    @Select("SELECT * FROM comments WHERE id = #{id}")
    Optional<Comment> findById(Long id);
    
    @Select("""
        SELECT 
            c.*,
            u.username,
            u.name as user_name
        FROM comments c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.task_id = #{taskId}
        ORDER BY c.created_at DESC
    """)
    List<Comment> findByTaskId(Long taskId);
    
    @Update("""
        UPDATE comments
        SET content = #{content},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void update(Comment comment);
    
    @Delete("DELETE FROM comments WHERE id = #{id}")
    void deleteById(Long id);
}

// ========================================

package com.example.taskapp.mapper;

import com.example.taskapp.entity.Tag;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface TagMapper {
    
    @Insert("""
        INSERT INTO tags (name, color)
        VALUES (#{name}, #{color})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Tag tag);
    
    @Select("SELECT * FROM tags WHERE id = #{id}")
    Optional<Tag> findById(Long id);
    
    @Select("SELECT * FROM tags ORDER BY name")
    List<Tag> findAll();
    
    @Select("""
        SELECT t.*
        FROM tags t
        JOIN task_tags tt ON t.id = tt.tag_id
        WHERE tt.task_id = #{taskId}
        ORDER BY t.name
    """)
    List<Tag> findByTaskId(Long taskId);
    
    @Delete("DELETE FROM tags WHERE id = #{id}")
    void deleteById(Long id);
}

// ========================================

package com.example.taskapp.mapper;

import com.example.taskapp.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Insert("""
        INSERT INTO users (username, email, password, name)
        VALUES (#{username}, #{email}, #{password}, #{name})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(String email);
    
    @Select("SELECT * FROM users ORDER BY username")
    List<User> findAll();
    
    @Update("""
        UPDATE users
        SET email = #{email},
            name = #{name},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void update(User user);
    
    @Update("""
        UPDATE users
        SET password = #{password},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    void updatePassword(@Param("id") Long id, @Param("password") String password);
}
