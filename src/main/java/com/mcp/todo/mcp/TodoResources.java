package com.mcp.todo.mcp;

import com.mcp.todo.model.Todo;
import com.mcp.todo.repository.TodoRepository;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Primitive #2 - Resources.
 *
 * <p>부작용 없이 "읽기 전용" 컨텍스트를 제공하는 리소스. Tool 이 행동이라면 Resource 는 데이터다.
 * 고정 URI {@code todos://list} 로 현재 전체 Todo 목록을 노출한다.
 * 메서드가 {@code String} 을 반환하면 선언한 {@code mimeType} 과 {@code uri} 로 감싸져
 * {@code ReadResourceResult} 로 Client 에게 전달된다.
 */
@Service
public class TodoResources {

    private final TodoRepository repository;

    public TodoResources(TodoRepository repository) {
        this.repository = repository;
    }

    @McpResource(
            uri = "todos://list",
            name = "todo-list",
            title = "전체 할 일 목록",
            description = "현재 등록된 모든 할 일(Todo)의 읽기 전용 스냅샷",
            mimeType = "text/plain")
    public String allTodos() {
        List<Todo> todos = repository.findAll();
        if (todos.isEmpty()) {
            return "등록된 할 일이 없습니다.";
        }

        String body = todos.stream()
                .map(todo -> "[%d] %s %s".formatted(
                        todo.id(),
                        todo.completed() ? "(완료)" : "(미완료)",
                        todo.title()))
                .collect(Collectors.joining("\n"));

        long done = todos.stream().filter(Todo::completed).count();
        return "총 %d개 (완료 %d / 미완료 %d)%n%s".formatted(todos.size(), done, todos.size() - done, body);
    }
}