package com.mulesoft.connectors.bedrock.internal.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("EmbeddingOperation")
class EmbeddingOperationTest {

  @Test
  @DisplayName("extends BedrockOperation and uses EmbeddingService")
  void extendsBedrockOperation() {
    EmbeddingOperation ops = new EmbeddingOperation();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new EmbeddingOperation()).isNotNull();
  }

  @Test
  @DisplayName("generateEmbedding returns InputStream with embedding JSON")
  void generateEmbeddingReturnsInputStream() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2]}")).build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v1");

    EmbeddingOperation ops = new EmbeddingOperation();
    InputStream result = ops.generateEmbedding(config, connection, params, "Sample text");

    assertThat(result).isNotNull();
    String content = new Scanner(result, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
    assertThat(content).contains("embedding");
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }
}
