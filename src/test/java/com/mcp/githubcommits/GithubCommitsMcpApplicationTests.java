package com.mcp.githubcommits;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// STDIO transport 는 System.in 을 점유하므로, 컨텍스트 로드 테스트에서는 비활성화한다.
@SpringBootTest(properties = "spring.ai.mcp.server.stdio=false")
class GithubCommitsMcpApplicationTests {

	@Test
	void contextLoads() {
	}

}