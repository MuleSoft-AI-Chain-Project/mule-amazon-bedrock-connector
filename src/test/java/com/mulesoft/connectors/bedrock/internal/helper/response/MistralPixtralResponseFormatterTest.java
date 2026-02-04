package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("MistralPixtralResponseFormatter")
class MistralPixtralResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Pixtral response with usage")
  void formatWithUsage() {
    JSONObject messageObj = new JSONObject();
    messageObj.put("content", " Hello Pixtral ");
    messageObj.put("role", "assistant");
    JSONObject choice = new JSONObject();
    choice.put("message", messageObj);
    choice.put("finish_reason", "stop");
    JSONObject usageObj = new JSONObject();
    usageObj.put("prompt_tokens", 5);
    usageObj.put("completion_tokens", 10);
    usageObj.put("total_tokens", 15);

    JSONObject raw = new JSONObject();
    raw.put("choices", new JSONArray().put(choice));
    raw.put("usage", usageObj);
    raw.put("amazon-bedrock-guardrailAction", "NONE");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    MistralPixtralResponseFormatter formatter = new MistralPixtralResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello Pixtral");
    assertThat(output).contains("usage");
    assertThat(output).contains("end_turn");
  }

  @Test
  @DisplayName("format handles missing usage object")
  void formatWithoutUsage() {
    JSONObject messageObj = new JSONObject();
    messageObj.put("content", "Response");
    messageObj.put("role", "assistant");
    JSONObject choice = new JSONObject();
    choice.put("message", messageObj);
    choice.put("finish_reason", "stop");

    JSONObject raw = new JSONObject();
    raw.put("choices", new JSONArray().put(choice));

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    MistralPixtralResponseFormatter formatter = new MistralPixtralResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("Response");
    assertThat(output).contains("output");
    assertThat(output).contains("usage");
  }
}
