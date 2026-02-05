package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.AutomationCredentials;
import com.mulesoft.connectors.bedrock.internal.support.BedrockConnectionTestHelper;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;

@Tag("integration")
@DisplayName("ChatService integration (real AWS)")
class ChatServiceIntegrationTest {

  private static BedrockConnection connection;
  private static BedrockConfiguration config;

  @BeforeAll
  static void setupConnection() throws Exception {
    AutomationCredentials creds = AutomationCredentials.load();
    Assumptions.assumeTrue(creds.isAvailable(), "automation-credentials.properties with config.* required");
    connection = BedrockConnectionTestHelper.createConnection(creds);
    config = new BedrockConfiguration();
    try {
      connection.validate();
    } catch (Exception e) {
      Assumptions.assumeTrue(false, "AWS credentials invalid or expired: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("answerPrompt returns non-empty response for short prompt")
  void answerPrompt() {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 50);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Say hello in one word.", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPrompt with different params returns non-empty response")
  void answerPromptWithDifferentParams() {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 30);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    String result = service.answerPrompt("Reply with the word OK only.", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("answerPromptStreaming returns SSE stream with session-start and chunks")
  void answerPromptStreaming() throws Exception {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.2f, 20);
    ChatServiceImpl service = new ChatServiceImpl(config, connection);
    try (InputStream stream = service.answerPromptStreaming("Say hi.", params);
        Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name())) {
      StringBuilder content = new StringBuilder();
      while (scanner.hasNextLine()) {
        content.append(scanner.nextLine()).append("\n");
      }
      String output = content.toString();
      assertThat(output).isNotBlank();
      assertThat(output).contains("event:");
      assertThat(output).contains("data:");
    }
  }
}
