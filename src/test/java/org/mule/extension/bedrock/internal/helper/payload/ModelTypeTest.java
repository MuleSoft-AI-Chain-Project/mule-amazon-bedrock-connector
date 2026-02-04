package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModelType")
class ModelTypeTest {

  @Test
  @DisplayName("all enum values exist")
  void allValues() {
    assertThat(ModelType.values()).containsExactlyInAnyOrder(
                                                             ModelType.TEXT,
                                                             ModelType.VISION,
                                                             ModelType.MODERATION,
                                                             ModelType.IMAGE);
  }
}
