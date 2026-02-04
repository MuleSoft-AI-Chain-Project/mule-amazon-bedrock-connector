package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.AutomationCredentials;
import com.mulesoft.connectors.bedrock.internal.support.BedrockConnectionTestHelper;

@Tag("integration")
@DisplayName("EmbeddingService integration (real AWS)")
class EmbeddingServiceIntegrationTest {

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
  @DisplayName("generateEmbeddings returns JSON with embedding array")
  void generateEmbeddings() throws Exception {
    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Sample text for embedding.", params);
    assertThat(result).isNotBlank();
    assertThat(result).contains("embedding");
  }

  @Test
  @DisplayName("generateEmbeddings with titan-embed-text-v2 and dimension/normalize returns embedding")
  void generateEmbeddingsTitanV2() throws Exception {
    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v2:0");
    setField(params, "dimension", 256);
    setField(params, "normalize", true);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Another sample for v2 embedding.", params);
    assertThat(result).isNotBlank();
    assertThat(result).contains("embedding");
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }
}
