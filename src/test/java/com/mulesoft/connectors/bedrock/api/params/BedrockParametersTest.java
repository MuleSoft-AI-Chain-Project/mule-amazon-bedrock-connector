package com.mulesoft.connectors.bedrock.api.params;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.api.enums.TimeUnitEnum;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockParameters getters")
class BedrockParametersTest {

  @Test
  @DisplayName("BedrockParameters getters return set values")
  void bedrockParametersGetters() {
    BedrockParameters p = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 100);
    assertThat(p.getModelName()).isEqualTo("amazon.nova-lite-v1:0");
    assertThat(p.getTemperature()).isEqualTo(0.5f);
    assertThat(p.getMaxTokenCount()).isEqualTo(100);
  }

  @Test
  @DisplayName("BedrockAgentsSessionParameters getters return set values")
  void sessionParamsGetters() {
    BedrockAgentsSessionParameters p = IntegrationTestParamHelper.sessionParams("sess-1", true, 2);
    assertThat(p.getSessionId()).isEqualTo("sess-1");
    assertThat(p.getExcludePreviousThinkingSteps()).isTrue();
    assertThat(p.getPreviousConversationTurnsToInclude()).isEqualTo(2);
  }

  @Test
  @DisplayName("BedrockAgentsResponseParameters getters return set values")
  void responseParamsGetters() {
    BedrockAgentsResponseParameters p =
        IntegrationTestParamHelper.responseParams(30, TimeUnitEnum.SECONDS, true, 3, 1000L);
    assertThat(p.getRequestTimeout()).isEqualTo(30);
    assertThat(p.getRequestTimeoutUnit()).isEqualTo(TimeUnitEnum.SECONDS);
    assertThat(p.getEnableRetry()).isTrue();
    assertThat(p.getMaxRetries()).isEqualTo(3);
    assertThat(p.getRetryBackoffMs()).isEqualTo(1000L);
  }

  @Test
  @DisplayName("BedrockParameters getTopP getTopK getGuardrail getAwsAccountId")
  void bedrockParamsOptionalGetters() throws Exception {
    BedrockParameters p = new BedrockParameters();
    IntegrationTestParamHelper.setField(p, "topP", 0.9f);
    IntegrationTestParamHelper.setField(p, "topK", 40);
    IntegrationTestParamHelper.setField(p, "guardrailIdentifier", "g-1");
    IntegrationTestParamHelper.setField(p, "guardrailVersion", "1.0");
    IntegrationTestParamHelper.setField(p, "awsAccountId", "123456789");
    assertThat(p.getTopP()).isEqualTo(0.9f);
    assertThat(p.getTopK()).isEqualTo(40);
    assertThat(p.getGuardrailIdentifier()).isEqualTo("g-1");
    assertThat(p.getGuardrailVersion()).isEqualTo("1.0");
    assertThat(p.getAwsAccountId()).isEqualTo("123456789");
  }
}
