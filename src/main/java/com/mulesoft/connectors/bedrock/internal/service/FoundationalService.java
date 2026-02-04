package com.mulesoft.connectors.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.params.BedrockParamsModelDetails;

public interface FoundationalService extends ConnectorService {

  String getFoundationModel(BedrockParamsModelDetails bedrockParamsModelDetails);

  String listFoundationModels();
}
