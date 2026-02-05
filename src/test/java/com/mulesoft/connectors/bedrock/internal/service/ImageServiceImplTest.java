package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockImageParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("ImageServiceImpl")
class ImageServiceImplTest {

  private static final String MINIMAL_BASE64_PNG =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    ImageServiceImpl service = new ImageServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements ImageService and extends BedrockServiceImpl")
  void typeHierarchy() {
    ImageServiceImpl service = new ImageServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(ImageService.class);
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }

  @Test
  @DisplayName("invokeModel returns file path when connection returns Stability image body")
  void invokeModelReturnsFilePath(@TempDir File tempDir) throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    String responseBody = "{\"artifacts\":[{\"base64\":\"" + MINIMAL_BASE64_PNG + "\"}]}";
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(responseBody)).build());

    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "modelName", "stability.stable-diffusion-xl-v1");
    setField(params, "numOfImages", 1);
    setField(params, "height", 512);
    setField(params, "width", 512);
    setField(params, "cfgScale", 7.0f);
    setField(params, "seed", 0);

    ImageServiceImpl service = new ImageServiceImpl(config, connection);
    File outFile = new File(tempDir, "out.png");
    String result = service.invokeModel("A cat", "", outFile.getAbsolutePath(), params);

    assertThat(result).isNotBlank();
    assertThat(new File(result)).exists();
  }

  @Test
  @DisplayName("invokeModel returns file path when connection returns Amazon Nova Canvas image body")
  void invokeModelAmazonNovaCanvas(@TempDir File tempDir) throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    String responseBody = "{\"images\":[\"" + MINIMAL_BASE64_PNG + "\"]}";
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(responseBody)).build());

    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "modelName", "amazon.nova-canvas-v1:0");
    setField(params, "numOfImages", 1);
    setField(params, "height", 512);
    setField(params, "width", 512);
    setField(params, "cfgScale", 7.0f);
    setField(params, "seed", 0);

    ImageServiceImpl service = new ImageServiceImpl(config, connection);
    File outFile = new File(tempDir, "nova.png");
    String result = service.invokeModel("A bird", "", outFile.getAbsolutePath(), params);

    assertThat(result).isNotBlank();
    assertThat(new File(result)).exists();
  }

  @Test
  @DisplayName("invokeModel returns null when connection throws")
  void invokeModelThrowsWhenConnectionThrows(@TempDir File tempDir) throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("network error").build());

    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "modelName", "stability.stable-diffusion-xl-v1");
    setField(params, "numOfImages", 1);
    setField(params, "height", 512);
    setField(params, "width", 512);

    ImageServiceImpl service = new ImageServiceImpl(config, connection);
    File outFile = new File(tempDir, "out.png");
    String result = service.invokeModel("A cat", "", outFile.getAbsolutePath(), params);
    org.assertj.core.api.Assertions.assertThat(result).isNull();
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }
}
