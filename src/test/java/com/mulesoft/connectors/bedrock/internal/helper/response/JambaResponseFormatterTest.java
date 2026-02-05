package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("JambaResponseFormatter")
class JambaResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Jamba response to standard output")
  void format() {
    JSONObject message = new JSONObject();
    message.put("role", "assistant");
    message.put("content", " Hello Jamba ");
    JSONObject choice = new JSONObject();
    choice.put("message", message);
    choice.put("finish_reason", "stop");
    JSONObject usage = new JSONObject();
    usage.put("prompt_tokens", 5);
    usage.put("completion_tokens", 15);
    usage.put("total_tokens", 20);

    JSONObject raw = new JSONObject();
    raw.put("choices", new JSONArray().put(choice));
    raw.put("usage", usage);
    raw.put("amazon-bedrock-guardrailAction", "NONE");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    JambaResponseFormatter formatter = new JambaResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello Jamba");
    assertThat(output).contains("usage");
    assertThat(output).contains("end_turn");
  }

  @Test
  @DisplayName("format uses default finish_reason when missing")
  void formatWithDefaultFinishReason() {
    JSONObject message = new JSONObject();
    message.put("role", "assistant");
    message.put("content", "Response");
    JSONObject choice = new JSONObject();
    choice.put("message", message);
    JSONObject usage = new JSONObject();
    usage.put("prompt_tokens", 1);
    usage.put("completion_tokens", 2);
    usage.put("total_tokens", 3);

    JSONObject raw = new JSONObject();
    raw.put("choices", new JSONArray().put(choice));
    raw.put("usage", usage);

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    JambaResponseFormatter formatter = new JambaResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("Response");
    assertThat(output).contains("end_turn");
  }
}
