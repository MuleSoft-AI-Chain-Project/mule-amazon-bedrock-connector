package com.mulesoft.connectors.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockModelFactory")
class BedrockModelFactoryTest {

  @Nested
  @DisplayName("createInputStream")
  class CreateInputStream {

    @Test
    @DisplayName("creates InputStream with UTF-8 content")
    void createsInputStreamWithContent() throws Exception {
      String input = "{\"key\":\"value\"}";
      InputStream stream = BedrockModelFactory.createInputStream(input);
      assertThat(stream).isNotNull();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[256];
      int n;
      while ((n = stream.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
      assertThat(out.toString(StandardCharsets.UTF_8.name())).isEqualTo(input);
    }

    @Test
    @DisplayName("handles empty string")
    void handlesEmptyString() throws Exception {
      InputStream stream = BedrockModelFactory.createInputStream("");
      assertThat(stream).isNotNull();
      assertThat(stream.read()).isEqualTo(-1);
    }
  }

  @Nested
  @DisplayName("identity")
  class Identity {

    @Test
    @DisplayName("returns same InputStream instance")
    void returnsSameInstance() {
      InputStream input = new InputStream() {

        @Override
        public int read() {
          return -1;
        }
      };
      assertThat(BedrockModelFactory.identity(input)).isSameAs(input);
    }
  }
}
