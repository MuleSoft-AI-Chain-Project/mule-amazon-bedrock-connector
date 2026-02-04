package com.mulesoft.connectors.bedrock.internal.operations;

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
import com.mulesoft.connectors.bedrock.api.params.BedrockParamsModelDetails;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.services.bedrock.model.FoundationModelDetails;
import software.amazon.awssdk.services.bedrock.model.FoundationModelLifecycle;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;

@DisplayName("FoundationalModelOperations")
class FoundationalModelOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses FoundationalService")
  void extendsBedrockOperation() {
    FoundationalModelOperations ops = new FoundationalModelOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new FoundationalModelOperations()).isNotNull();
  }

  @Test
  @DisplayName("getFoundationModelDetails returns InputStream with model JSON")
  void getFoundationModelDetailsReturnsInputStream() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    FoundationModelDetails details = FoundationModelDetails.builder()
        .modelId("amazon.titan-embed-text-v1")
        .modelArn("arn:aws:bedrock:us-east-1::foundation-model/amazon.titan-embed-text-v1")
        .modelName("Titan Embed Text")
        .providerName("Amazon")
        .modelLifecycle(FoundationModelLifecycle.builder().status("ACTIVE").build())
        .build();
    when(connection.getFoundationModel(any())).thenReturn(GetFoundationModelResponse.builder().modelDetails(details).build());

    BedrockParamsModelDetails params = new BedrockParamsModelDetails();
    setField(params, "modelName", "amazon.titan-embed-text-v1");

    FoundationalModelOperations ops = new FoundationalModelOperations();
    InputStream result = ops.getFoundationModelDetails(config, connection, params);

    assertThat(result).isNotNull();
    String content = new Scanner(result, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
    assertThat(content).contains("amazon.titan-embed-text-v1");
  }

  @Test
  @DisplayName("listFoundationModels returns InputStream with JSON array")
  void listFoundationModelsReturnsInputStream() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    FoundationModelSummary summary = FoundationModelSummary.builder()
        .modelId("amazon.nova-lite-v1")
        .modelArn("arn:aws:bedrock:us-east-1::foundation-model/amazon.nova-lite-v1")
        .modelName("Nova Lite")
        .providerName("Amazon")
        .modelLifecycle(FoundationModelLifecycle.builder().status("ACTIVE").build())
        .build();
    when(connection.listFoundationalModels()).thenReturn(ListFoundationModelsResponse.builder().modelSummaries(summary).build());

    FoundationalModelOperations ops = new FoundationalModelOperations();
    InputStream result = ops.listFoundationModels(config, connection);

    assertThat(result).isNotNull();
    String content = new Scanner(result, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
    assertThat(content).startsWith("[");
    assertThat(content).endsWith("]");
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }
}
