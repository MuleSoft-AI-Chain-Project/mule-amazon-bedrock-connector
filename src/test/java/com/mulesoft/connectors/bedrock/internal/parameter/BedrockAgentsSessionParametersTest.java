package com.mulesoft.connectors.bedrock.internal.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsSessionParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockAgentsSessionParameters")
class BedrockAgentsSessionParametersTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("can be instantiated")
  void canBeInstantiated() {
    BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
    assertThat(params).isNotNull();
  }

  @Test
  @DisplayName("getSessionId returns set value")
  void getSessionId() throws Exception {
    BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
    setField(params, "sessionId", "session-123");
    assertThat(params.getSessionId()).isEqualTo("session-123");
  }

  @Test
  @DisplayName("getExcludePreviousThinkingSteps returns set value")
  void getExcludePreviousThinkingSteps() throws Exception {
    BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
    setField(params, "excludePreviousThinkingSteps", true);
    assertThat(params.getExcludePreviousThinkingSteps()).isTrue();
  }

  @Test
  @DisplayName("getPreviousConversationTurnsToInclude returns set value")
  void getPreviousConversationTurnsToInclude() throws Exception {
    BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
    setField(params, "previousConversationTurnsToInclude", 5);
    assertThat(params.getPreviousConversationTurnsToInclude()).isEqualTo(5);
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSame() {
      BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
      assertThat(params.equals(params)).isTrue();
    }

    @Test
    @DisplayName("equals returns true for equal instances")
    void equalsReturnsTrueForEqual() throws Exception {
      BedrockAgentsSessionParameters p1 = new BedrockAgentsSessionParameters();
      setField(p1, "sessionId", "session-123");
      setField(p1, "excludePreviousThinkingSteps", true);
      setField(p1, "previousConversationTurnsToInclude", 5);

      BedrockAgentsSessionParameters p2 = new BedrockAgentsSessionParameters();
      setField(p2, "sessionId", "session-123");
      setField(p2, "excludePreviousThinkingSteps", true);
      setField(p2, "previousConversationTurnsToInclude", 5);

      assertThat(p1.equals(p2)).isTrue();
      assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different instances")
    void equalsReturnsFalseForDifferent() throws Exception {
      BedrockAgentsSessionParameters p1 = new BedrockAgentsSessionParameters();
      setField(p1, "sessionId", "session-123");

      BedrockAgentsSessionParameters p2 = new BedrockAgentsSessionParameters();
      setField(p2, "sessionId", "session-456");

      assertThat(p1.equals(p2)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
      assertThat(params.equals(null)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for different type")
    void equalsReturnsFalseForDifferentType() {
      BedrockAgentsSessionParameters params = new BedrockAgentsSessionParameters();
      assertThat(params.equals("string")).isFalse();
    }
  }
}
