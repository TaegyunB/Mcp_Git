package com.mcp.todo.model;

/**
 * 최소 필드만 가지는 Todo 도메인 모델.
 *
 * <p>불변(immutable) record 로 정의하여, 완료 처리 시에는 새로운 인스턴스로 교체한다.
 * 별도의 DB 없이 메모리에만 저장되므로 서버가 재시작되면 초기화된다.
 *
 * @param id        Todo 고유 식별자 (서버가 자동 채번)
 * @param title     할 일 제목
 * @param completed 완료 여부
 */
public record Todo(long id, String title, boolean completed) {

    /** 완료 상태로 표시한 새 Todo 를 반환한다. */
    public Todo markCompleted() {
        return new Todo(id, title, true);
    }
}