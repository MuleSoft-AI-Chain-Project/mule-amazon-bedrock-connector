package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParamsModelDetails;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.services.bedrock.model.FoundationModelDetails;
import software.amazon.awssdk.services.bedrock.model.FoundationModelLifecycle;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

@DisplayName("FoundationalServiceImpl")
class FoundationalServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements FoundationalService")
  void implementsFoundationalService() {
    FoundationalServiceImpl service =
        new FoundationalServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(FoundationalService.class);
  }

  @Nested
  @DisplayName("getFoundationModel")
  class GetFoundationModel {

    @Test
    @DisplayName("returns JSON with model details when connection returns response")
    void returnsJsonFromConnection() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);

      FoundationModelDetails details = FoundationModelDetails.builder()
          .modelId("amazon.titan-text-express-v1")
          .modelArn("arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-text-express-v1")
          .modelName("Titan Text Express")
          .providerName("Amazon")
          .modelLifecycle(FoundationModelLifecycle.builder().status("ACTIVE").build())
          .build();

      GetFoundationModelResponse response = GetFoundationModelResponse.builder()
          .modelDetails(details)
          .build();

      when(connection.getFoundationModel(any())).thenReturn(response);

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      BedrockParamsModelDetails params = mock(BedrockParamsModelDetails.class);
      when(params.getModelName()).thenReturn("amazon.titan-text-express-v1");

      String result = service.getFoundationModel(params);

      assertThat(result).isNotBlank();
      assertThat(result).contains("amazon.titan-text-express-v1");
      assertThat(result).contains("Titan Text Express");
      assertThat(result).contains("Amazon");
    }
  }

  @Nested
  @DisplayName("listFoundationModels")
  class ListFoundationModels {

    @Test
    @DisplayName("returns JSON array when connection returns models")
    void returnsJsonArrayFromConnection() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);

      FoundationModelSummary summary = FoundationModelSummary.builder()
          .modelId("amazon.titan-text-express-v1")
          .modelArn("arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-text-express-v1")
          .modelName("Titan Text Express")
          .providerName("Amazon")
          .modelLifecycle(FoundationModelLifecycle.builder().status("ACTIVE").build())
          .build();

      ListFoundationModelsResponse response = ListFoundationModelsResponse.builder()
          .modelSummaries(summary)
          .build();

      when(connection.listFoundationalModels()).thenReturn(response);

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      String result = service.listFoundationModels();

      assertThat(result).isNotBlank();
      assertThat(result).startsWith("[");
      assertThat(result).endsWith("]");
      assertThat(result).contains("amazon.titan-text-express-v1");
    }

    @Test
    @DisplayName("returns empty array when connection returns no models")
    void returnsEmptyArrayWhenNoModels() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);
      when(connection.listFoundationalModels()).thenReturn(ListFoundationModelsResponse.builder().build());

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      String result = service.listFoundationModels();

      assertThat(result).isEqualTo("[]");
    }

    @Test
    @DisplayName("throws RuntimeException when connection throws SdkClientException")
    void throwsWhenSdkClientException() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);
      when(connection.listFoundationalModels())
          .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("client error").build());

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      org.assertj.core.api.Assertions.assertThatThrownBy(service::listFoundationModels)
          .isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("getFoundationModel error paths")
  class GetFoundationModelErrors {

    @Test
    @DisplayName("throws IllegalArgumentException when connection throws ValidationException")
    void throwsIllegalArgumentExceptionWhenValidationException() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);
      when(connection.getFoundationModel(any()))
          .thenThrow(software.amazon.awssdk.services.bedrock.model.ValidationException.builder().message("invalid model")
              .build());

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      BedrockParamsModelDetails params = mock(BedrockParamsModelDetails.class);
      when(params.getModelName()).thenReturn("bad.model");

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getFoundationModel(params))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("throws RuntimeException when connection throws SdkException")
    void throwsRuntimeExceptionWhenSdkException() {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);
      when(connection.getFoundationModel(any()))
          .thenThrow(software.amazon.awssdk.core.exception.SdkException.builder().message("sdk error").build());

      FoundationalServiceImpl service = new FoundationalServiceImpl(config, connection);
      BedrockParamsModelDetails params = mock(BedrockParamsModelDetails.class);
      when(params.getModelName()).thenReturn("amazon.nova-lite-v1:0");

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getFoundationModel(params))
          .isInstanceOf(RuntimeException.class);
    }
  }
}
