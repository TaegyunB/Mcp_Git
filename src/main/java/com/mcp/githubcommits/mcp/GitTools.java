package com.mcp.githubcommits.mcp;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitTools {

    @McpTool(name = "git_add", description = "지정한 로컬 git 저장소의 모든 변경사항을 스테이징한다 (git add -A).")
    public String gitAdd(
            @McpArg(name = "repoPath", description = "로컬 git 저장소 경로", required = true) String repoPath) {
        return runGit(repoPath, "add", "-A");
    }

    @McpTool(name = "git_commit", description = "스테이징된 변경사항을 주어진 메시지로 커밋한다 (git commit -m).")
    public String gitCommit(
            @McpArg(name = "repoPath", description = "로컬 git 저장소 경로", required = true) String repoPath,
            @McpArg(name = "message", description = "커밋 메시지", required = true) String message) {
        return runGit(repoPath, "commit", "-m", message);
    }

    @McpTool(name = "git_push",
            description = "현재 브랜치를 원격 저장소로 push 한다. 원격 저장소 상태를 변경하는 되돌리기 어려운 작업이므로, "
                    + "실행 전 반드시 사용자에게 어떤 저장소/브랜치에 push할지 확인받은 뒤 호출할 것.")
    public String gitPush(
            @McpArg(name = "repoPath", description = "로컬 git 저장소 경로", required = true) String repoPath) {
        return runGit(repoPath, "push");
    }

    private String runGit(String repoPath, String... args) {
        File dir = new File(repoPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("존재하지 않는 디렉터리입니다: " + repoPath);
        }

        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        try {
            Process process = new ProcessBuilder(command)
                    .directory(dir)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        "git %s 실패 (exit %d): %s".formatted(String.join(" ", args), exitCode, output));
            }
            return output.isBlank() ? "OK" : output;
        } catch (IOException e) {
            throw new RuntimeException("git 명령 실행 중 오류: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("git 명령 실행이 중단되었습니다.", e);
        }
    }
}
