package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("ClaudeResponseFormatter")
class ClaudeResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Claude response to standard output")
  void format() {
    JSONObject content0 = new JSONObject();
    content0.put("text", " Hello Claude ");
    JSONArray contentArray = new JSONArray().put(content0);
    JSONObject usage = new JSONObject();
    usage.put("input_tokens", 10);
    usage.put("output_tokens", 20);

    JSONObject raw = new JSONObject();
    raw.put("content", contentArray);
    raw.put("role", "assistant");
    raw.put("usage", usage);
    raw.put("stop_reason", "end_turn");
    raw.put("amazon-bedrock-guardrailAction", "NONE");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    ClaudeResponseFormatter formatter = new ClaudeResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello Claude");
    assertThat(output).contains("usage");
    assertThat(output).contains("end_turn");
  }

  @Test
  @DisplayName("format uses default stop_reason and guardrail when missing")
  void formatWithDefaults() {
    JSONObject content0 = new JSONObject();
    content0.put("text", "Response");
    JSONObject raw = new JSONObject();
    raw.put("content", new JSONArray().put(content0));
    raw.put("role", "assistant");
    raw.put("usage", new JSONObject().put("input_tokens", 1).put("output_tokens", 2));

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    ClaudeResponseFormatter formatter = new ClaudeResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("Response");
    assertThat(output).contains("end_turn");
  }
}
