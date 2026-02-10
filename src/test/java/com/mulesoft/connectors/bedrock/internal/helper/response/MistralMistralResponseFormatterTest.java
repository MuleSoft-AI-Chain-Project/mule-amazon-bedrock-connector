package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("MistralMistralResponseFormatter")
class MistralMistralResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Mistral response to standard output")
  void format() {
    JSONObject output0 = new JSONObject();
    output0.put("text", " Hello Mistral ");
    output0.put("stop_reason", "stop");
    JSONObject raw = new JSONObject();
    raw.put("outputs", new JSONArray().put(output0));
    raw.put("amazon-bedrock-guardrailAction", "NONE");

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    MistralMistralResponseFormatter formatter = new MistralMistralResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello Mistral");
    assertThat(output).contains("usage");
    assertThat(output).contains("end_turn");
  }

  @Test
  @DisplayName("format uses default stop_reason when missing")
  void formatWithDefaultStopReason() {
    JSONObject output0 = new JSONObject();
    output0.put("text", "Done");
    JSONObject raw = new JSONObject();
    raw.put("outputs", new JSONArray().put(output0));

    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(raw.toString()))
        .build();

    MistralMistralResponseFormatter formatter = new MistralMistralResponseFormatter();
    String output = formatter.format(response);

    assertThat(output).contains("Done");
    assertThat(output).contains("end_turn");
  }
}
