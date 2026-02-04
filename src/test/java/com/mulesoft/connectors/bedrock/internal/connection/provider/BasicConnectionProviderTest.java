package com.mulesoft.connectors.bedrock.internal.connection.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BasicConnectionProvider")
class BasicConnectionProviderTest {

  @Test
  @DisplayName("can be instantiated")
  void constructor() {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    assertThat(provider).isNotNull();
  }
}
