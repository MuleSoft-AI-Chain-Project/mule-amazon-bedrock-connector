package com.mulesoft.connectors.bedrock.internal.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockImageParameters")
class BedrockImageParametersTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("can be instantiated")
  void canBeInstantiated() {
    BedrockImageParameters params = new BedrockImageParameters();
    assertThat(params).isNotNull();
  }

  @Test
  @DisplayName("getModelName returns set value")
  void getModelName() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "modelName", "test-model");
    assertThat(params.getModelName()).isEqualTo("test-model");
  }

  @Test
  @DisplayName("getNumOfImages returns set value")
  void getNumOfImages() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "numOfImages", 3);
    assertThat(params.getNumOfImages()).isEqualTo(3);
  }

  @Test
  @DisplayName("getHeight returns set value")
  void getHeight() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "height", 1024);
    assertThat(params.getHeight()).isEqualTo(1024);
  }

  @Test
  @DisplayName("getWidth returns set value")
  void getWidth() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "width", 768);
    assertThat(params.getWidth()).isEqualTo(768);
  }

  @Test
  @DisplayName("getCfgScale returns set value")
  void getCfgScale() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "cfgScale", 10.5f);
    assertThat(params.getCfgScale()).isEqualTo(10.5f);
  }

  @Test
  @DisplayName("getSeed returns set value")
  void getSeed() throws Exception {
    BedrockImageParameters params = new BedrockImageParameters();
    setField(params, "seed", 42);
    assertThat(params.getSeed()).isEqualTo(42);
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSame() {
      BedrockImageParameters params = new BedrockImageParameters();
      assertThat(params.equals(params)).isTrue();
    }

    @Test
    @DisplayName("equals returns true for equal instances")
    void equalsReturnsTrueForEqual() throws Exception {
      BedrockImageParameters p1 = new BedrockImageParameters();
      setField(p1, "modelName", "model");
      setField(p1, "numOfImages", 1);
      setField(p1, "height", 512);
      setField(p1, "width", 512);
      setField(p1, "cfgScale", 8.0f);
      setField(p1, "seed", 0);

      BedrockImageParameters p2 = new BedrockImageParameters();
      setField(p2, "modelName", "model");
      setField(p2, "numOfImages", 1);
      setField(p2, "height", 512);
      setField(p2, "width", 512);
      setField(p2, "cfgScale", 8.0f);
      setField(p2, "seed", 0);

      assertThat(p1.equals(p2)).isTrue();
      assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different instances")
    void equalsReturnsFalseForDifferent() throws Exception {
      BedrockImageParameters p1 = new BedrockImageParameters();
      setField(p1, "modelName", "model1");

      BedrockImageParameters p2 = new BedrockImageParameters();
      setField(p2, "modelName", "model2");

      assertThat(p1.equals(p2)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      BedrockImageParameters params = new BedrockImageParameters();
      assertThat(params.equals(null)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for different type")
    void equalsReturnsFalseForDifferentType() {
      BedrockImageParameters params = new BedrockImageParameters();
      assertThat(params.equals("string")).isFalse();
    }
  }
}
