package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("EmbeddingServiceImpl")
class EmbeddingServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements EmbeddingService")
  void implementsEmbeddingService() {
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(EmbeddingService.class);
  }

  @Test
  @DisplayName("generateEmbeddings returns JSON when connection returns embedding body")
  void generateEmbeddingsReturnsJson() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    String body = "{\"embedding\":[0.1,0.2,0.3]}";
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Sample text.", params);

    assertThat(result).isNotBlank();
    assertThat(result).contains("embedding");
  }

  @Test
  @DisplayName("generateEmbeddings with cohere.embed model returns embedding")
  void generateEmbeddingsCohere() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embeddings\":[[0.1,0.2]]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "cohere.embed-multilingual-v3");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Text for cohere.", params);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("generateEmbeddings with amazon.titan-embed-text-v2 returns embedding")
  void generateEmbeddingsTitanV2() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2,0.3]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v2:0");
    setField(params, "dimension", 256);
    setField(params, "normalize", true);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Text.", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("generateEmbeddings with amazon.titan-embed-image returns embedding")
  void generateEmbeddingsTitanImage() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-image-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Image text.", params);
    assertThat(result).isNotBlank();
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Nested
  @DisplayName("removeEmptyStrings")
  class RemoveEmptyStrings {

    @Test
    @DisplayName("removes empty strings from array")
    void removesEmptyStrings() {
      String[] input = {"a", "", "b", "", "c"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("returns empty array when all elements are empty")
    void allEmpty() {
      String[] input = {"", "", ""};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns same array when no empty strings")
    void noEmptyStrings() {
      String[] input = {"x", "y", "z"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("x", "y", "z");
    }

    @Test
    @DisplayName("handles single element array")
    void singleElement() {
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {"only"})).containsExactly("only");
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {""})).isEmpty();
    }
  }
}
