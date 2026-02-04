package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParamsModelDetails;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.support.AutomationCredentials;
import com.mulesoft.connectors.bedrock.internal.support.BedrockConnectionTestHelper;

@Tag("integration")
@DisplayName("FoundationalService integration (real AWS)")
class FoundationalServiceIntegrationTest {

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
  @DisplayName("listFoundationModels returns non-empty JSON array")
  void listFoundationModels() {
    FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
    String result = service.listFoundationModels();
    assertThat(result).isNotBlank();
    assertThat(result).startsWith("[");
    assertThat(result).endsWith("]");
  }

  @Test
  @DisplayName("getFoundationModel returns model details JSON for amazon.nova-lite-v1:0")
  void getFoundationModel() throws Exception {
    BedrockParamsModelDetails params = new BedrockParamsModelDetails();
    Field f = BedrockParamsModelDetails.class.getDeclaredField("modelName");
    f.setAccessible(true);
    f.set(params, "amazon.nova-lite-v1:0");
    FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
    String result = service.getFoundationModel(params);
    assertThat(result).isNotBlank();
    assertThat(result).contains("amazon.nova-lite");
  }

  @Test
  @DisplayName("getFoundationModel returns model details for amazon.titan-embed-text-v1")
  void getFoundationModelTitanEmbed() throws Exception {
    BedrockParamsModelDetails params = new BedrockParamsModelDetails();
    Field f = BedrockParamsModelDetails.class.getDeclaredField("modelName");
    f.setAccessible(true);
    f.set(params, "amazon.titan-embed-text-v1");
    FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
    String result = service.getFoundationModel(params);
    assertThat(result).isNotBlank();
    assertThat(result).contains("modelId");
    assertThat(result).contains("titan-embed");
  }

  @Test
  @DisplayName("getFoundationModel throws for invalid model id")
  void getFoundationModelInvalidModel() throws Exception {
    BedrockParamsModelDetails params = new BedrockParamsModelDetails();
    Field f = BedrockParamsModelDetails.class.getDeclaredField("modelName");
    f.setAccessible(true);
    f.set(params, "invalid.model.id.that.does.not.exist");
    FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
    org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> service.getFoundationModel(params));
  }
}
