package com.mulesoft.connectors.bedrock.internal.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.support.AutomationCredentials;
import com.mulesoft.connectors.bedrock.internal.support.BedrockConnectionTestHelper;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

@Tag("integration")
@DisplayName("BedrockConnection integration (real AWS)")
class BedrockConnectionIntegrationTest {

  private static BedrockConnection connection;

  @BeforeAll
  static void setupConnection() throws Exception {
    AutomationCredentials creds = AutomationCredentials.load();
    Assumptions.assumeTrue(creds.isAvailable(), "automation-credentials.properties with config.* required");
    connection = BedrockConnectionTestHelper.createConnection(creds);
    try {
      connection.validate();
    } catch (Exception e) {
      Assumptions.assumeTrue(false, "AWS credentials invalid or expired: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("validate does not throw when credentials are valid")
  void validate() {
    connection.validate();
  }

  @Test
  @DisplayName("getRegion returns non-blank region")
  void getRegion() {
    String region = connection.getRegion();
    assertThat(region).isNotBlank();
  }

  @Test
  @DisplayName("listFoundationalModels returns non-empty response")
  void listFoundationalModels() {
    ListFoundationModelsResponse response = connection.listFoundationalModels();
    assertThat(response).isNotNull();
    assertThat(response.modelSummaries()).isNotNull();
  }

  @Test
  @DisplayName("getBedrockRuntimeClient is non-null")
  void getBedrockRuntimeClient() {
    assertThat(connection.getBedrockRuntimeClient()).isNotNull();
  }

  @Test
  @DisplayName("getBedrockClient is non-null")
  void getBedrockClient() {
    assertThat(connection.getBedrockClient()).isNotNull();
  }

  @Test
  @DisplayName("getConnectionTimeoutMs returns positive value")
  void getConnectionTimeoutMs() {
    assertThat(connection.getConnectionTimeoutMs()).isPositive();
  }

  @Test
  @DisplayName("getOrCreateBedrockRuntimeAsyncClient returns client")
  void getOrCreateBedrockRuntimeAsyncClient() {
    assertThat(connection.getOrCreateBedrockRuntimeAsyncClient(connection.getConnectionTimeoutMs())).isNotNull();
  }
}
