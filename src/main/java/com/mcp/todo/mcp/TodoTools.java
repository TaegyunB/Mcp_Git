package com.mcp.todo.mcp;

import com.mcp.todo.model.Todo;
import com.mcp.todo.repository.TodoRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Primitive #1 - Tools.
 *
 * <p>Host(예: Claude Desktop)의 요청으로 실제 "행동"을 수행하는, 모델이 호출 가능한 도구들.
 * {@code @McpTool} 이 붙은 public 메서드는 서버 기동 시 자동으로 스캔되어 MCP 도구로 노출된다.
 * 파라미터는 {@code @McpToolParam} 정보와 파라미터 타입으로부터 JSON Schema 가 생성되고,
 * 반환값은 구조화된(structured) 결과로 직렬화되어 Client 에게 전달된다.
 */
@Service
public class TodoTools {

    private final TodoRepository repository;

    public TodoTools(TodoRepository repository) {
        this.repository = repository;
    }

    @McpTool(name = "add_todo", description = "제목을 받아 새로운 할 일(Todo)을 추가하고, 생성된 Todo 를 반환한다.")
    public Todo addTodo(
            @McpToolParam(description = "추가할 할 일의 제목", required = true) String title) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title 은 비어 있을 수 없습니다.");
        }
        return repository.add(title.trim());
    }

    @McpTool(name = "complete_todo", description = "id 를 받아 해당 할 일(Todo)을 완료 처리하고, 갱신된 Todo 를 반환한다.")
    public Todo completeTodo(
            @McpToolParam(description = "완료 처리할 할 일의 id", required = true) long id) {

        return repository.complete(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 id 의 할 일을 찾을 수 없습니다: " + id));
    }
}