package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("LlamaResponseFormatter")
class LlamaResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Llama response to standard output")
  void format() {
    JSONObject raw = new JSONObject();
    raw.put("generation", " Hello Llama ");
    raw.put("prompt_token_count", 5);
    raw.put("generation_token_count", 10);
    raw.put("stop_reason", "stop");
    raw.put("amazon-bedrock-guardrailAction", "NONE");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    LlamaResponseFormatter formatter = new LlamaResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello Llama");
    assertThat(output).contains("usage");
    assertThat(output).contains("end_turn");
  }

  @Test
  @DisplayName("format preserves non-stop stop_reason via normalizeStopReason")
  void formatWithCustomStopReason() {
    JSONObject raw = new JSONObject();
    raw.put("generation", "Done");
    raw.put("prompt_token_count", 1);
    raw.put("generation_token_count", 2);
    raw.put("stop_reason", "length");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    LlamaResponseFormatter formatter = new LlamaResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("Done");
    assertThat(output).contains("length");
  }
}
