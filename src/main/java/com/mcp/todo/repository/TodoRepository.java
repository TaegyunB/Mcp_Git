package com.mcp.todo.repository;

import com.mcp.todo.model.Todo;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 기반 Todo 저장소.
 *
 * <p>DB 없이 {@link ConcurrentHashMap} 에만 저장하므로 서버 재시작 시 초기화된다.
 * MCP 서버는 여러 요청을 동시에 처리할 수 있으므로 스레드 안전한 자료구조를 사용한다.
 */
@Repository
public class TodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    /** 새 Todo 를 추가하고 생성된 Todo 를 반환한다. */
    public Todo add(String title) {
        long id = sequence.incrementAndGet();
        Todo todo = new Todo(id, title, false);
        store.put(id, todo);
        return todo;
    }

    /**
     * 주어진 id 의 Todo 를 완료 처리한다.
     *
     * @return 완료 처리된 Todo, 해당 id 가 없으면 {@link Optional#empty()}
     */
    public Optional<Todo> complete(long id) {
        return Optional.ofNullable(store.computeIfPresent(id, (k, v) -> v.markCompleted()));
    }

    /** id 오름차순으로 정렬된 전체 Todo 목록을 반환한다. */
    public List<Todo> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparingLong(Todo::id))
                .toList();
    }
}