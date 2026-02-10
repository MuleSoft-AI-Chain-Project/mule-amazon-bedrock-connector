package com.mulesoft.connectors.bedrock.internal.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockParametersEmbeddingDocument")
class BedrockParametersEmbeddingDocumentTest {

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("can be instantiated")
  void canBeInstantiated() {
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    assertThat(params).isNotNull();
  }

  @Test
  @DisplayName("getModelName returns set value")
  void getModelName() throws Exception {
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    assertThat(params.getModelName()).isEqualTo("amazon.titan-embed-text-v1");
  }

  @Test
  @DisplayName("getDimension returns set value")
  void getDimension() throws Exception {
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    setField(params, "dimension", 512);
    assertThat(params.getDimension()).isEqualTo(512);
  }

  @Test
  @DisplayName("getNormalize returns set value")
  void getNormalize() throws Exception {
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    setField(params, "normalize", true);
    assertThat(params.getNormalize()).isTrue();
  }

  @Test
  @DisplayName("getOptionType returns set value")
  void getOptionType() throws Exception {
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    setField(params, "optionType", "PARAGRAPH");
    assertThat(params.getOptionType()).isEqualTo("PARAGRAPH");
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equals returns true for same instance")
    void equalsReturnsTrueForSame() {
      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      assertThat(params.equals(params)).isTrue();
    }

    @Test
    @DisplayName("equals returns true for equal instances")
    void equalsReturnsTrueForEqual() throws Exception {
      BedrockParametersEmbeddingDocument p1 = new BedrockParametersEmbeddingDocument();
      setField(p1, "modelName", "model");
      setField(p1, "dimension", 256);
      setField(p1, "normalize", true);
      setField(p1, "optionType", "FULL");

      BedrockParametersEmbeddingDocument p2 = new BedrockParametersEmbeddingDocument();
      setField(p2, "modelName", "model");
      setField(p2, "dimension", 256);
      setField(p2, "normalize", true);
      setField(p2, "optionType", "FULL");

      assertThat(p1.equals(p2)).isTrue();
      assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("equals returns false for different instances")
    void equalsReturnsFalseForDifferent() throws Exception {
      BedrockParametersEmbeddingDocument p1 = new BedrockParametersEmbeddingDocument();
      setField(p1, "modelName", "model1");

      BedrockParametersEmbeddingDocument p2 = new BedrockParametersEmbeddingDocument();
      setField(p2, "modelName", "model2");

      assertThat(p1.equals(p2)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for null")
    void equalsReturnsFalseForNull() {
      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      assertThat(params.equals(null)).isFalse();
    }

    @Test
    @DisplayName("equals returns false for different type")
    void equalsReturnsFalseForDifferentType() {
      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      assertThat(params.equals("string")).isFalse();
    }
  }
}
