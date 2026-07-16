# github-commits-mcp

GitHub 계정의 오늘자 커밋을 조회하고, 로컬 저장소의 git 작업(status/diff/add/commit/push)과 GitHub 이슈 생성을 지원하는 MCP(Model Context Protocol) 서버.

Claude 같은 MCP 클라이언트가 이 서버에 연결하면, 아래 tool/resource/prompt를 통해 LLM이 GitHub 활동 조회, 로컬 커밋 작업, 이슈 등록을 대신 수행할 수 있다.

## 기술 스택

- Java 17
- Spring Boot 4.1.0
- Spring AI MCP Server (WebMVC, `spring-ai-starter-mcp-server-webmvc` 2.0.0)
- Lombok
- Maven (Wrapper 포함 — `./mvnw`)

## 아키텍처

MCP 서버는 **Streamable HTTP** transport로 동작한다 (stdio 비활성화). `@McpTool` / `@McpResource` / `@McpPrompt` 어노테이션이 붙은 메서드를 Spring AI가 애플리케이션 기동 시 자동으로 스캔·등록하며, 클라이언트가 표준 MCP 메서드(`tools/list`, `tools/call`, `resources/list`, `resources/read`, `prompts/list`, `prompts/get`)로 요청하면 해당 메서드가 실행된다.

```
com.mcp.githubcommits
├── GithubCommitsMcpApplication   # Spring Boot 엔트리포인트
└── mcp
    ├── GithubTools     # GitHub API 연동 tool (오늘 커밋 조회, 이슈 생성)
    ├── GitTools        # 로컬 git 명령 실행 tool (status/diff/add/commit/push)
    ├── GitResources    # 커밋 메시지 컨벤션 문서 (resource)
    └── GithubPrompts   # 이슈 생성용 프롬프트 템플릿 (prompt)
```

## 제공 기능

### Tools

| 이름 | 설명 | 파라미터 |
|---|---|---|
| `get_todays_commits` | GitHub 계정(`TaegyunB`)이 오늘(한국시간 기준) 올린 모든 레포지토리의 커밋과 각 커밋의 변경 파일 목록을 조회한다. | 없음 |
| `create_issue` | 지정한 레포지토리(`owner/repo`)에 제목/본문으로 이슈를 생성한다. | `repo`, `title`, `body` |
| `git_status` | 로컬 저장소의 변경 파일 목록을 조회한다 (`git status --short`). | `repoPath` |
| `git_diff` | 로컬 저장소의 변경 내용을 조회한다 (`git diff HEAD`, staged/unstaged 모두 포함). | `repoPath` |
| `git_add` | 모든 변경사항을 스테이징한다 (`git add -A`). | `repoPath` |
| `git_commit` | 스테이징된 변경사항을 커밋한다 (`git commit -m`). | `repoPath`, `message` |
| `git_push` | 현재 브랜치를 원격 저장소로 push한다. 되돌리기 어려운 작업이라 실행 전 사용자 확인을 거치도록 설계됨. | `repoPath` |

`get_todays_commits`는 KST 자정~자정 범위로 GitHub Search Commits API(`GET /search/commits`)를 `author:` + `committer-date:` 조건으로 호출하고, 커밋마다 상세 API(`GET /repos/{repo}/commits/{sha}`)를 추가 호출해 변경 파일 목록을 채운다.

### Resources

| URI | 이름 | 설명 |
|---|---|---|
| `guide://commit-convention` | 커밋 메시지 컨벤션 | Conventional Commits 기반의 이 프로젝트 커밋 메시지 작성 규칙(`text/markdown`). `git_diff`로 변경 내용을 확인한 뒤, 이 문서를 참고해 규칙에 맞는 커밋 메시지를 작성하도록 돕는다. |

### Prompts

| 이름 | 설명 | 파라미터 |
|---|---|---|
| `create_issue` | 배경/작업 내용/기대 결과 형식으로 이슈 본문을 정리해 `create_issue` tool 호출을 안내하는 프롬프트. | `repo`, `title`, `body` |

### 권장 작업 흐름 (커밋)

```
git_status / git_diff  → 변경 내용 확인
git_add                → 스테이징
guide://commit-convention → 컨벤션 확인
(LLM이 메시지 작성)
git_commit
git_push (사용자 확인 후)
```

## 설정 (`application.yaml`)

- Transport: Streamable HTTP, 엔드포인트 `/mcp`, 포트 `8080`
- Capabilities: `tool`, `resource`, `prompt`, `completion` 활성화
- 인증: `GITHUB_TOKEN` 환경변수로 주입되는 GitHub Personal Access Token을 서버가 자체 보유해 사용 (사용자별 별도 OAuth 동의 절차 없음)
- 로그: `./logs/github-commits-mcp-server.log`에 파일 로그 기록 (HTTP transport라 stdout을 통신 채널로 쓰지 않으므로 콘솔 로그 확인용)

## 실행 방법

### 사전 준비

```bash
# GitHub PAT 발급 후 환경변수로 설정 (repo, workflow 등 필요한 스코프 부여)
export GITHUB_TOKEN=ghp_xxxxxxxxxxxx
```

### 빌드

```bash
./mvnw clean package -DskipTests
```

### 실행

```bash
./mvnw spring-boot:run
# 또는
java -jar target/github-commits-mcp-0.0.1-SNAPSHOT.jar
```

정상 기동 시 `http://localhost:8080/mcp`에서 MCP 요청을 받는다.

> 이미 8080 포트를 점유한 프로세스가 있으면 기동에 실패한다. 아래 명령으로 기존 프로세스를 정리한 뒤 재시도한다.
>
> ```powershell
> Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*github-commits*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
> ```

## MCP 클라이언트 연결

이 서버는 HTTP transport로 동작하므로 연결 방식이 클라이언트마다 다르다.

- **Claude Desktop 커스텀 커넥터**: Anthropic 클라우드가 직접 접속하는 구조라 **공개적으로 도달 가능한 HTTPS 주소**(CA 서명 인증서)가 필요하다. 로컬에서 테스트하려면 ngrok 등으로 `localhost:8080`을 공개 HTTPS URL로 터널링해야 한다.
- **Claude Code (CLI)**: HTTP transport를 네이티브로 지원해 로컬 주소를 바로 등록할 수 있다.

  ```bash
  claude mcp add --transport http github-commits http://localhost:8080/mcp
  ```

## 주의사항

- `git_push`, `create_issue`는 원격 상태를 바꾸는 되돌리기 어려운 작업이므로, LLM이 실행 전 사용자 확인을 거치도록 tool description에 명시돼 있다.
- `GITHUB_TOKEN`은 서버가 자체적으로 보유하는 구조라, 이 서버에 접근 가능한 누구나 해당 토큰 권한으로 GitHub API를 호출할 수 있다. 공개 URL로 노출할 경우 접근 제어에 유의한다.