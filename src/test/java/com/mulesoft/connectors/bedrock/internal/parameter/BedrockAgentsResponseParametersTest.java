package com.mulesoft.connectors.bedrock.internal.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockAgentsResponseParameters")
class BedrockAgentsResponseParametersTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("can be instantiated")
  void canBeInstantiated() {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    assertThat(params).isNotNull();
  }

  @Test
  @DisplayName("getRequestTimeout returns set value")
  void getRequestTimeout() throws Exception {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    setField(params, "requestTimeout", 30);
    assertThat(params.getRequestTimeout()).isEqualTo(30);
  }

  @Test
  @DisplayName("getRequestTimeoutUnit returns set value")
  void getRequestTimeoutUnit() throws Exception {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    setField(params, "requestTimeoutUnit", TimeUnit.SECONDS);
    assertThat(params.getRequestTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("getEnableRetry returns set value")
  void getEnableRetry() throws Exception {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    setField(params, "enableRetry", true);
    assertThat(params.getEnableRetry()).isTrue();
  }

  @Test
  @DisplayName("getMaxRetries returns set value")
  void getMaxRetries() throws Exception {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    setField(params, "maxRetries", 5);
    assertThat(params.getMaxRetries()).isEqualTo(5);
  }

  @Test
  @DisplayName("getRetryBackoffMs returns set value")
  void getRetryBackoffMs() throws Exception {
    BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
    setField(params, "retryBackoffMs", 2000L);
    assertThat(params.getRetryBackoffMs()).isEqualTo(2000L);
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSame() {
      BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
      assertThat(params.equals(params)).isTrue();
    }

    @Test
    @DisplayName("equals returns true for equal instances")
    void equalsReturnsTrueForEqual() throws Exception {
      BedrockAgentsResponseParameters p1 = new BedrockAgentsResponseParameters();
      setField(p1, "requestTimeout", 30);
      setField(p1, "requestTimeoutUnit", TimeUnit.SECONDS);
      setField(p1, "enableRetry", true);
      setField(p1, "maxRetries", 3);
      setField(p1, "retryBackoffMs", 1000L);

      BedrockAgentsResponseParameters p2 = new BedrockAgentsResponseParameters();
      setField(p2, "requestTimeout", 30);
      setField(p2, "requestTimeoutUnit", TimeUnit.SECONDS);
      setField(p2, "enableRetry", true);
      setField(p2, "maxRetries", 3);
      setField(p2, "retryBackoffMs", 1000L);

      assertThat(p1.equals(p2)).isTrue();
      assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different instances")
    void equalsReturnsFalseForDifferent() throws Exception {
      BedrockAgentsResponseParameters p1 = new BedrockAgentsResponseParameters();
      setField(p1, "enableRetry", true);

      BedrockAgentsResponseParameters p2 = new BedrockAgentsResponseParameters();
      setField(p2, "enableRetry", false);

      assertThat(p1.equals(p2)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
      assertThat(params.equals(null)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for different type")
    void equalsReturnsFalseForDifferentType() {
      BedrockAgentsResponseParameters params = new BedrockAgentsResponseParameters();
      assertThat(params.equals("string")).isFalse();
    }
  }
}
