package com.mcp.githubcommits.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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

    public record CommitInfo(String sha, String repository, String message, String url, List<String> changedFiles) {
    }

    @McpTool(name = "get_todays_commits",
            description = "GitHub 계정(TaegyunB)이 오늘(한국시간 기준) 올린 모든 레포지토리의 커밋 목록과 각 커밋의 변경 파일 목록을 가져온다.")
    public List<CommitInfo> getTodaysCommits() {
        requireToken();

        LocalDate today = LocalDate.now(KST);
        Instant since = today.atStartOfDay(KST).toInstant();
        Instant until = today.plusDays(1).atStartOfDay(KST).minusSeconds(1).toInstant();

        String query = "author:%s committer-date:%s..%s".formatted(
                GITHUB_USERNAME,
                DateTimeFormatter.ISO_INSTANT.format(since),
                DateTimeFormatter.ISO_INSTANT.format(until));

        String url = "https://api.github.com/search/commits?per_page=100&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);

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

    private List<String> fetchChangedFiles(String repoFullName, String sha) {
        String url = "https://api.github.com/repos/%s/commits/%s".formatted(repoFullName, sha);
        JsonNode root = get(url);
        List<String> files = new ArrayList<>();
        for (JsonNode file : root.path("files")) {
            files.add(file.path("filename").asString());
        }
        return files;
    }

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

    private void requireToken() {
        if (githubToken == null || githubToken.isBlank()) {
            throw new IllegalStateException("GITHUB_TOKEN 환경변수가 설정되어 있지 않습니다.");
        }
    }
}