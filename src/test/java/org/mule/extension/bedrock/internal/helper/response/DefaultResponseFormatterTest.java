package org.mule.extension.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("DefaultResponseFormatter")
class DefaultResponseFormatterTest {

  @Test
  @DisplayName("format returns body as UTF-8 string")
  void formatReturnsBody() {
    String json = "{\"output\":{\"text\":\"Hi\"},\"stopReason\":\"end_turn\"}";
    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(json))
        .build();
    DefaultResponseFormatter formatter = new DefaultResponseFormatter();
    String result = formatter.format(response);
    assertThat(result).isNotNull();
    assertThat(result).contains("output");
    assertThat(result).contains("Hi");
  }
}
