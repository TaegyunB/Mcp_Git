package com.mcp.todo.mcp;

import com.mcp.todo.model.Todo;
import com.mcp.todo.repository.TodoRepository;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Primitive #3 - Prompts.
 *
 * <p>재사용 가능한 프롬프트 템플릿. Host 의 사용자가 "슬래시 명령"처럼 선택해 쓸 수 있는,
 * 미리 정의된 대화 시작점이다. 현재 Todo 목록을 채워 넣어 "오늘 할 일을 요약해줘" 요청을
 * 완성된 {@link GetPromptResult} 형태로 돌려준다.
 */
@Service
public class TodoPrompts {

    private final TodoRepository repository;

    public TodoPrompts(TodoRepository repository) {
        this.repository = repository;
    }

    @McpPrompt(
            name = "summarize_todos",
            title = "오늘 할 일 요약",
            description = "현재 등록된 할 일 목록을 바탕으로 오늘 할 일을 요약해 달라고 요청하는 프롬프트")
    public GetPromptResult summarizeTodos(
            @McpArg(name = "tone",
                    description = "요약 말투 (예: 정중하게, 간결하게). 지정하지 않으면 간결하게 요약한다.",
                    required = false) String tone) {

        String effectiveTone = (tone == null || tone.isBlank()) ? "간결하게" : tone.trim();

        List<Todo> todos = repository.findAll();
        String todoList = todos.isEmpty()
                ? "(등록된 할 일이 없습니다.)"
                : todos.stream()
                        .map(todo -> "- [%s] %s".formatted(todo.completed() ? "완료" : "미완료", todo.title()))
                        .collect(Collectors.joining("\n"));

        String text = """
                다음은 현재 등록된 할 일 목록입니다. 오늘 할 일을 %s 요약해 주세요.
                완료된 항목과 남은 항목을 구분하고, 우선순위가 높아 보이는 일을 짚어 주세요.

                [할 일 목록]
                %s
                """.formatted(effectiveTone, todoList);

        PromptMessage message = new PromptMessage(Role.USER, new TextContent(text));
        return new GetPromptResult("오늘 할 일 요약 프롬프트", List.of(message));
    }
}
