package org.mule.extension.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("StringUtils")
class StringUtilsTest {

  @Nested
  @DisplayName("isPresent")
  class IsPresent {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("returns false for null and empty string")
    void returnsFalseForNullAndEmpty(String input) {
      assertThat(StringUtils.isPresent(input)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "hello", " ", "  "})
    @DisplayName("returns true for non-empty string")
    void returnsTrueForNonEmpty(String input) {
      assertThat(StringUtils.isPresent(input)).isTrue();
    }

    @Test
    @DisplayName("returns true for string with content")
    void returnsTrueForContent() {
      assertThat(StringUtils.isPresent("content")).isTrue();
    }
  }
}
