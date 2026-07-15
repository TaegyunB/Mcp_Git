package com.mcp.githubcommits.mcp;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool - GitHub 오늘 커밋 조회.
 *
 * <p>GitHub Search Commits API 를 이용해 특정 계정이 오늘(한국시간 기준) 올린
 * 모든 레포지토리의 커밋을 한 번에 가져온다. 레포를 고정하지 않으므로 여러 레포에
 * 흩어져 있는 알고리즘 문제 풀이도 함께 조회된다.
 *
 * <p>인증은 GITHUB_TOKEN 환경변수로 주입받은 Personal Access Token 을 서버가
 * 자체적으로 들고 있다가 사용한다 (사용자 별도 동의 절차 없음).
 */
@Service
public class GithubTools {

    private static final String GITHUB_USERNAME = "TaegyunB";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final String githubToken;

    public GithubTools() {
        this.githubToken = System.getenv("GITHUB_TOKEN");
    }

    // record: 데이터를 저장하고 전달하는 용도로만 사용하는 '불변 데이터 객체'를 만들기 위한 종류의 클래스
    // 각 Tool의 응답을 담는 불변 DTO
    // @McpTool 메서드가 이 타입을 리턴하면 MCP 서버가 자동으로 JSON으로 직렬화해서 클라이언트에 넘김
    public record CommitInfo(String sha, String repository, String message, String url, List<String> changedFiles) {
    }

    public record IssueInfo(String repository, int number, String url) {
    }

    @McpTool(name = "get_todays_commits",
            description = "GitHub 계정(TaegyunB)이 오늘(한국시간 기준) 올린 모든 레포지토리의 커밋 목록과 각 커밋의 변경 파일 목록을 가져온다.")
    public List<CommitInfo> getTodaysCommits() {
        requireToken();

        LocalDate today = LocalDate.now(KST);
        Instant since = today.atStartOfDay(KST).toInstant();
        Instant until = today.plusDays(1).atStartOfDay(KST).minusSeconds(1).toInstant();

        // GitHub Search Commits API 쿼리 문자열 조립
        String query = "author:%s committer-date:%s..%s".formatted(
                GITHUB_USERNAME,
                DateTimeFormatter.ISO_INSTANT.format(since),
                DateTimeFormatter.ISO_INSTANT.format(until));

        String url = "https://api.github.com/search/commits?per_page=100&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);

        // 검색 API 호출
        JsonNode root = get(url);

        List<CommitInfo> result = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            String sha = item.path("sha").asString();
            String repo = item.path("repository").path("full_name").asString();
            String message = item.path("commit").path("message").asString();
            String htmlUrl = item.path("html_url").asString();
            result.add(new CommitInfo(sha, repo, message, htmlUrl, fetchChangedFiles(repo, sha)));
        }
        return result;
    }

    @McpTool(name = "create_issue",
            description = "지정한 GitHub 레포지토리(owner/repo 형식)에 제목과 본문으로 이슈를 생성한다.")
    public IssueInfo createIssue(
            @McpArg(name = "repo", description = "이슈를 생성할 레포지토리 (예: TaegyunB/todo)", required = true) String repo,
            @McpArg(name = "title", description = "이슈 제목", required = true) String title,
            @McpArg(name = "body", description = "이슈 본문", required = true) String body) {
        requireToken();

        ObjectNode payload = jsonMapper.createObjectNode();
        payload.put("title", title);
        payload.put("body", body);

        String url = "https://api.github.com/repos/%s/issues".formatted(repo);
        JsonNode root = post(url, jsonMapper.writeValueAsString(payload));

        return new IssueInfo(repo, root.path("number").asInt(), root.path("html_url").asString());
    }

    private List<String> fetchChangedFiles(String repoFullName, String sha) {
        String url = "https://api.github.com/repos/%s/commits/%s".formatted(repoFullName, sha);
        JsonNode root = get(url);
        List<String> files = new ArrayList<>();
        for (JsonNode file : root.path("files")) {
            files.add(file.path("filename").asString());
        }
        return files;
    }

    //== 공통 HTTP 헬퍼 ==//
    private JsonNode get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + githubToken)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "GitHub API 호출 실패 (%d): %s".formatted(response.statusCode(), response.body()));
            }
            return jsonMapper.readTree(response.body());
        } catch (java.io.IOException e) {
            throw new RuntimeException("GitHub API 호출 중 오류: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GitHub API 호출이 중단되었습니다.", e);
        }
    }

    private JsonNode post(String url, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + githubToken)
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException(
                        "GitHub API 호출 실패 (%d): %s".formatted(response.statusCode(), response.body()));
            }
            return jsonMapper.readTree(response.body());
        } catch (java.io.IOException e) {
            throw new RuntimeException("GitHub API 호출 중 오류: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GitHub API 호출이 중단되었습니다.", e);
        }
    }

    private void requireToken() {
        if (githubToken == null || githubToken.isBlank()) {
            throw new IllegalStateException("GITHUB_TOKEN 환경변수가 설정되어 있지 않습니다.");
        }
    }
}