package org.mule.extension.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.extension.aws.connection.provider.parameters.CommonParameters;
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
  }
}
