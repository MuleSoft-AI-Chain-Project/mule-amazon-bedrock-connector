package com.mulesoft.connectors.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("TitanResponseFormatter")
class TitanResponseFormatterTest {

  @Test
  @DisplayName("format normalizes Titan response to standard output")
  void format() {
    JSONObject result0 = new JSONObject();
    result0.put("outputText", " Hello world ");
    result0.put("completionReason", "FINISH");
    result0.put("tokenCount", 5);
    org.json.JSONArray resultsArray = new org.json.JSONArray().put(result0);
    JSONObject raw = new JSONObject();
    raw.put("results", resultsArray);
    raw.put("inputTextTokenCount", 10);
    String body = raw.toString();
    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(body))
        .build();
    TitanResponseFormatter formatter = new TitanResponseFormatter();
    String output = formatter.format(response);
    assertThat(output).contains("output");
    assertThat(output).contains("message");
    assertThat(output).contains("Hello world");
    assertThat(output).contains("end_turn");
    assertThat(output).contains("usage");
  }

  @Test
  @DisplayName("format uses unknown stop reason when completionReason is not FINISH")
  void formatWithNonFinishCompletionReason() {
    JSONObject result0 = new JSONObject();
    result0.put("outputText", "Partial");
    result0.put("completionReason", "LENGTH");
    result0.put("tokenCount", 3);
    org.json.JSONArray resultsArray = new org.json.JSONArray().put(result0);
    JSONObject raw = new JSONObject();
    raw.put("results", resultsArray);
    raw.put("inputTextTokenCount", 2);
    String body = raw.toString();
    InvokeModelResponse response = InvokeModelResponse.builder()
        .body(SdkBytes.fromUtf8String(body))
        .build();
    TitanResponseFormatter formatter = new TitanResponseFormatter();
    String output = formatter.format(response);
    assertThat(output).contains("Partial");
    assertThat(output).contains("unknown");
  }
}
