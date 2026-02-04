package com.mulesoft.connectors.bedrock.internal.service;

import org.mule.connectors.commons.template.service.DefaultConnectorService;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;

public class BedrockServiceImpl extends DefaultConnectorService<BedrockConfiguration, BedrockConnection> {

  public BedrockServiceImpl(BedrockConfiguration bedrockConfiguration, BedrockConnection bedrockConnection) {
    super(bedrockConfiguration, bedrockConnection);
  }
}
