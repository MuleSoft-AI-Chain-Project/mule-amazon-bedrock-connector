package org.mule.extension.bedrock.internal.helper.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResponseFormatterFactory")
class ResponseFormatterFactoryTest {

  @Test
  @DisplayName("getFormatter returns formatter for each response group")
  void getFormatterReturnsForGroups() {
    assertThat(ResponseFormatterFactory.getFormatter("anthropic.claude-3")).isNotNull();
    assertThat(ResponseFormatterFactory.getFormatter("mistral.pixtral")).isNotNull();
    assertThat(ResponseFormatterFactory.getFormatter("mistral.mistral-7b")).isNotNull();
    assertThat(ResponseFormatterFactory.getFormatter("ai21.jamba")).isNotNull();
    assertThat(ResponseFormatterFactory.getFormatter("meta.llama3")).isNotNull();
    assertThat(ResponseFormatterFactory.getFormatter("amazon.titan-text")).isNotNull();
  }

  @Test
  @DisplayName("getFormatter returns default for unknown model")
  void getFormatterReturnsDefaultForUnknown() {
    ResponseFormatter formatter = ResponseFormatterFactory.getFormatter("unknown.model.xyz");
    assertThat(formatter).isNotNull();
    assertThat(formatter).isInstanceOf(DefaultResponseFormatter.class);
  }

  @Test
  @DisplayName("getFormatter returns default for null and blank")
  void getFormatterHandlesNullAndBlank() {
    ResponseFormatter f1 = ResponseFormatterFactory.getFormatter(null);
    ResponseFormatter f2 = ResponseFormatterFactory.getFormatter("");
    assertThat(f1).isNotNull();
    assertThat(f2).isNotNull();
  }
}
