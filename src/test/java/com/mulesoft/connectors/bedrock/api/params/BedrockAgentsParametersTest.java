package com.mulesoft.connectors.bedrock.api.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockAgentsParameters")
class BedrockAgentsParametersTest {

  @Test
  @DisplayName("getModelName and getRegion return null when not set")
  void getters() {
    BedrockAgentsParameters params = new BedrockAgentsParameters();
    assertThat(params.getModelName()).isNull();
    assertThat(params.getRegion()).isNull();
  }
}
