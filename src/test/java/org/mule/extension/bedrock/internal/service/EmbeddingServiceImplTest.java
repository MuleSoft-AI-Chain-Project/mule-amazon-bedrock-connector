package org.mule.extension.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;

@DisplayName("EmbeddingServiceImpl")
class EmbeddingServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements EmbeddingService")
  void implementsEmbeddingService() {
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(EmbeddingService.class);
  }

  @Nested
  @DisplayName("removeEmptyStrings")
  class RemoveEmptyStrings {

    @Test
    @DisplayName("removes empty strings from array")
    void removesEmptyStrings() {
      String[] input = {"a", "", "b", "", "c"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("returns empty array when all elements are empty")
    void allEmpty() {
      String[] input = {"", "", ""};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns same array when no empty strings")
    void noEmptyStrings() {
      String[] input = {"x", "y", "z"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("x", "y", "z");
    }

    @Test
    @DisplayName("handles single element array")
    void singleElement() {
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {"only"})).containsExactly("only");
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {""})).isEmpty();
    }
  }
}
