package com.mulesoft.connectors.bedrock.internal.service;

import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import org.mule.connectors.commons.template.service.DefaultConnectorService;

public class BedrockServiceImpl extends DefaultConnectorService<BedrockConfiguration, BedrockConnection> {

  public BedrockServiceImpl(BedrockConfiguration bedrockConfiguration, BedrockConnection bedrockConnection) {
    super(bedrockConfiguration, bedrockConnection);
  }
}
