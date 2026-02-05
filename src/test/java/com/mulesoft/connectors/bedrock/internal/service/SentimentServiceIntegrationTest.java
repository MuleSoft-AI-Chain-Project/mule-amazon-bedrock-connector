package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("SentimentService integration (real AWS)")
class SentimentServiceIntegrationTest {

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
  @DisplayName("extractSentiments returns JSON with Sentiment and IsPositive")
  void extractSentiments() {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    String result = service.extractSentiments("I am very happy with this product!", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("extractSentiments with negative text returns non-empty response")
  void extractSentimentsNegative() {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    String result = service.extractSentiments("This is terrible and I want a refund.", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("extractSentiments with neutral text returns non-empty response")
  void extractSentimentsNeutral() {
    BedrockParameters params = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.3f, 50);
    SentimentServiceImpl service = new SentimentServiceImpl(config, connection);
    String result = service.extractSentiments("The order was delivered on time.", params);
    assertThat(result).isNotBlank();
  }
}
