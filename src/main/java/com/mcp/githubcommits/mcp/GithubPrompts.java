package com.mcp.githubcommits.mcp;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Service;

@Service
public class GithubPrompts {

    @McpPrompt(name = "create_issue",
            description = "GitHub 이슈 제목/내용을 정해진 형식으로 작성하고 create_issue 도구로 등록하도록 안내하는 프롬프트")
    public String createIssuePrompt(
            @McpArg(name = "repo", description = "이슈를 생성할 레포지토리 (예: TaegyunB/todo)", required = true) String repo,
            @McpArg(name = "title", description = "이슈 제목", required = true) String title,
            @McpArg(name = "body", description = "이슈 본문", required = true) String body) {
        return """
                %s 레포지토리에 올릴 GitHub 이슈를 생성해줘

                제목: %s
                
                아래 내용을 다음 형식으로 정리해서 본문을 구성한 뒤, create_issue 도구를 호출해 실제로 이슈를 생성해줘.
                - 배경: 왜 이 이슈가 필요한지
                - 작업 내용 또는 재현 방법: 구체적인 단계
                - 기대 결과: 완료 조건
                
                참고 내용:
                %s
                """.formatted(repo, title, body);
    }
}