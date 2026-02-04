package com.mulesoft.connectors.bedrock.internal.connection.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssumeRoleConnectionProvider")
class AssumeRoleConnectionProviderTest {

  @Test
  @DisplayName("can be instantiated")
  void constructor() {
    AssumeRoleConnectionProvider provider = new AssumeRoleConnectionProvider();
    assertThat(provider).isNotNull();
  }
}
