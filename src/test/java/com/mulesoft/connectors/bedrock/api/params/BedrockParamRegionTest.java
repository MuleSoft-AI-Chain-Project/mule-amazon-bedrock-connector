package com.mulesoft.connectors.bedrock.api.params;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockParamRegion")
class BedrockParamRegionTest {

  @Test
  @DisplayName("getRegion returns null when not set")
  void getRegion() {
    BedrockParamRegion params = new BedrockParamRegion();
    assertThat(params.getRegion()).isNull();
  }
}
