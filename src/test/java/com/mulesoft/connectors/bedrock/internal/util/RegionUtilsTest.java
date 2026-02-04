package com.mulesoft.connectors.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@DisplayName("RegionUtils")
class RegionUtilsTest {

  @Nested
  @DisplayName("getRegion")
  class GetRegion {

    @Test
    @DisplayName("returns normalized region from CommonParameters")
    void returnsRegion() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      assertThat(RegionUtils.getRegion(params)).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("normalizes region with underscore to hyphen")
    void normalizesRegion() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us_east_1");
      assertThat(RegionUtils.getRegion(params)).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("returns region as-is when Region.of throws")
    void returnsRegionWhenInvalid() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("invalid-region-id");
      assertThat(RegionUtils.getRegion(params)).isEqualTo("invalid-region-id");
    }

    @Test
    @DisplayName("returns region as-is when Region.of throws for null")
    void returnsRegionWhenNull() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn(null);
      assertThat(RegionUtils.getRegion(params)).isNull();
    }
  }

  @Nested
  @DisplayName("configureRegionProperty")
  class ConfigureRegionProperty {

    @Test
    @DisplayName("configures BedrockRuntimeClientBuilder with region when no custom endpoint")
    void configuresWithoutCustomEndpoint() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      RegionUtils.configureRegionProperty(BedrockRuntimeClient.builder(), params);
      // no exception = success
    }

    @Test
    @DisplayName("configures with custom service endpoint when set")
    void configuresWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      RegionUtils.configureRegionProperty(BedrockRuntimeClient.builder(), params);
      // no exception = success
    }

    @Test
    @DisplayName("configures BedrockClientBuilder with region")
    void configuresBedrockClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-west-2");
      RegionUtils.configureRegionProperty(BedrockClient.builder(), params);
      // no exception = success
    }

    @Test
    @DisplayName("configures with region only when custom endpoint is empty string")
    void configuresWithEmptyEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("");
      RegionUtils.configureRegionProperty(BedrockRuntimeClient.builder(), params);
      // no exception = success (else branch: region only)
    }

    @Test
    @DisplayName("configures BedrockAgentClientBuilder with region")
    void configuresBedrockAgentClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      RegionUtils.configureRegionProperty(
                                          software.amazon.awssdk.services.bedrockagent.BedrockAgentClient.builder(), params);
    }

    @Test
    @DisplayName("configures BedrockAgentRuntimeClientBuilder with region")
    void configuresBedrockAgentRuntimeClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      RegionUtils.configureRegionProperty(
                                          software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient.builder(),
                                          params);
    }

    @Test
    @DisplayName("configures IamClientBuilder with region")
    void configuresIamClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      RegionUtils.configureRegionProperty(
                                          software.amazon.awssdk.services.iam.IamClient.builder(), params);
    }
  }
}
