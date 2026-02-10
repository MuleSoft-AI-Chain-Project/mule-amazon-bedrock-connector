package com.mulesoft.connectors.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@DisplayName("RegionUtils")
class RegionUtilsTest {

  @Nested
  @DisplayName("getRegion")
  class GetRegion {

    @Test
    @DisplayName("returns Region from CommonParameters")
    void returnsRegion() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      assertThat(RegionUtils.getRegion(params)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    @DisplayName("normalizes region with underscore to hyphen")
    void normalizesRegion() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us_east_1");
      assertThat(RegionUtils.getRegion(params)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    @DisplayName("returns Region for any non-empty region id (SDK accepts custom region strings)")
    void returnsRegionForCustomId() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("invalid-region-id");
      assertThat(RegionUtils.getRegion(params).id()).isEqualTo("invalid-region-id");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when region is null")
    void throwsWhenNull() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn(null);
      assertThatThrownBy(() -> RegionUtils.getRegion(params))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("region must not be null or empty");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when commonParameters is null")
    void throwsWhenCommonParametersNull() {
      assertThatThrownBy(() -> RegionUtils.getRegion(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("commonParameters must not be null");
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
      var builder = BedrockRuntimeClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures with custom service endpoint when set")
    void configuresWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = BedrockRuntimeClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockClientBuilder with region")
    void configuresBedrockClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-west-2");
      var builder = BedrockClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures with region only when custom endpoint is empty string")
    void configuresWithEmptyEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("");
      var builder = BedrockRuntimeClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockAgentClientBuilder with region")
    void configuresBedrockAgentClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      var builder = software.amazon.awssdk.services.bedrockagent.BedrockAgentClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockAgentRuntimeClientBuilder with region")
    void configuresBedrockAgentRuntimeClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      var builder = software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures IamClientBuilder with region")
    void configuresIamClientBuilder() {
      CommonParameters params = new CommonParameters();
      params.setRegion("us-east-1");
      var builder = software.amazon.awssdk.services.iam.IamClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockClientBuilder with custom endpoint")
    void configuresBedrockClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = BedrockClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockAgentClientBuilder with custom endpoint")
    void configuresBedrockAgentClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = software.amazon.awssdk.services.bedrockagent.BedrockAgentClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockAgentRuntimeClientBuilder with custom endpoint")
    void configuresBedrockAgentRuntimeClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures IamClientBuilder with custom endpoint")
    void configuresIamClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = software.amazon.awssdk.services.iam.IamClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockAgentRuntimeAsyncClientBuilder with custom endpoint")
    void configuresBedrockAgentRuntimeAsyncClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }

    @Test
    @DisplayName("configures BedrockRuntimeAsyncClientBuilder with custom endpoint")
    void configuresBedrockRuntimeAsyncClientBuilderWithCustomEndpoint() {
      CommonParameters params = mock(CommonParameters.class);
      when(params.getRegion()).thenReturn("us-east-1");
      when(params.getCustomServiceEndpoint()).thenReturn("https://custom.bedrock.example.com");
      var builder = software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient.builder();
      RegionUtils.configureRegionProperty(builder, params);
      assertThat(builder.build()).isNotNull();
    }
  }
}
