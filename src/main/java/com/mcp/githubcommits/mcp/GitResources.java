package com.mcp.githubcommits.mcp;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

/**
 * MCP Resource - 커밋 메시지 컨벤션 문서.
 *
 * <p>Tool 이 아니라 Resource 인 이유: 인자에 따라 매번 다른 결과를 계산하는 '동작'이 아니라,
 * URI 로 지목되는 고정된 참고 문서이기 때문이다. LLM 이 git_diff 로 변경 내용을 확인한 뒤,
 * 이 리소스를 컨텍스트로 참조해 규칙에 맞는 커밋 메시지를 작성하고 git_commit 을 호출하는 흐름을 돕는다.
 */
@Service
public class GitResources {

    @McpResource(
            uri = "guide://commit-convention",
            name = "commit-convention",
            title = "커밋 메시지 컨벤션",
            description = "이 프로젝트의 커밋 메시지 작성 규칙(Conventional Commits 기반). "
                    + "자동 커밋 메시지를 생성하기 전에 이 문서를 참고할 것.",
            mimeType = "text/markdown")
    public String commitConvention() {
        return """
                # 커밋 메시지 컨벤션

                이 프로젝트는 Conventional Commits 형식을 따른다.

                ## 형식

                ```
                <type>: <제목>

                <본문(선택)>
                ```

                - **제목(첫 줄)**: `type: 설명` 형태로 한 줄, 50자 이내를 권장한다.
                - 제목은 한국어로, 명령형/요약형으로 작성한다. (예: "추가", "수정", "제거")
                - 제목 끝에 마침표(`.`)를 찍지 않는다.
                - 본문이 필요하면 제목과 빈 줄 하나로 구분한 뒤 '무엇을/왜' 바꿨는지 설명한다.

                ## type 종류

                | type | 용도 |
                |------|------|
                | `feat` | 새로운 기능 추가 |
                | `fix` | 버그 수정 |
                | `refactor` | 기능 변화 없는 코드 구조 개선 |
                | `docs` | 문서(주석/README 등)만 변경 |
                | `test` | 테스트 코드 추가/수정 |
                | `chore` | 빌드/설정/의존성 등 그 외 잡무 |
                | `style` | 포매팅, 세미콜론 등 동작에 영향 없는 변경 |

                ## 예시

                ```
                feat: git diff 조회 tool 추가
                fix: KST 기준 오늘 커밋 조회 범위 오류 수정
                refactor: git 명령 실행 로직을 runGit 헬퍼로 분리
                docs: 커밋 컨벤션 resource 설명 보강
                ```

                ## 작성 가이드

                1. `git_diff` / `git_status` 로 실제 변경 내용을 먼저 확인한다.
                2. 변경의 성격에 맞는 `type` 을 하나 고른다. 성격이 섞였으면 가장 핵심적인 변경을 기준으로 한다.
                3. 무엇을 바꿨는지 한 줄로 요약해 제목을 만든다.
                4. 변경 이유나 맥락이 필요하면 본문에 덧붙인다.
                """;
    }
}
