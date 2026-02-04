package com.mulesoft.connectors.bedrock.internal.support;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;

/**
 * Sets BedrockParameters fields via reflection for integration tests (param classes have no public setters).
 */
public final class IntegrationTestParamHelper {

  private IntegrationTestParamHelper() {}

  public static BedrockParameters bedrockParams(String modelName, Float temperature, Integer maxTokenCount) {
    BedrockParameters p = new BedrockParameters();
    set(p, "modelName", modelName);
    set(p, "temperature", temperature != null ? temperature : 0.7f);
    set(p, "maxTokenCount", maxTokenCount != null ? maxTokenCount : 100);
    return p;
  }

  public static BedrockAgentsSessionParameters sessionParams(String sessionId, boolean excludeThinking, Integer turns) {
    BedrockAgentsSessionParameters p = new BedrockAgentsSessionParameters();
    set(p, "sessionId", sessionId);
    set(p, "excludePreviousThinkingSteps", excludeThinking);
    set(p, "previousConversationTurnsToInclude", turns);
    return p;
  }

  public static BedrockAgentsResponseParameters responseParams(Integer timeout,
                                                               TimeUnit unit,
                                                               boolean enableRetry,
                                                               Integer maxRetries,
                                                               Long backoffMs) {
    BedrockAgentsResponseParameters p = new BedrockAgentsResponseParameters();
    set(p, "requestTimeout", timeout);
    set(p, "requestTimeoutUnit", unit);
    set(p, "enableRetry", enableRetry);
    set(p, "maxRetries", maxRetries != null ? maxRetries : 3);
    set(p, "retryBackoffMs", backoffMs != null ? backoffMs : 1000L);
    return p;
  }

  public static void setField(Object target, String fieldName, Object value) {
    set(target, fieldName, value);
  }

  private static void set(Object target, String fieldName, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Could not set " + fieldName + " on " + target.getClass().getSimpleName(), e);
    }
  }
}
