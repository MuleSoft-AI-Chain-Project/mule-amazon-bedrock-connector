package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockServiceImpl")
class BedrockServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    BedrockServiceImpl service = new BedrockServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("extends DefaultConnectorService")
  void extendsDefaultConnectorService() {
    BedrockServiceImpl service = new BedrockServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(BedrockServiceImpl.class);
  }
}
