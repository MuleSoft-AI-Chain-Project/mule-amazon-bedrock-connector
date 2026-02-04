package com.mulesoft.connectors.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.params.BedrockImageParameters;

public interface ImageService extends ConnectorService {

  String invokeModel(String prompt, String avoidInImage, String fullPath,
                     BedrockImageParameters awsBedrockParameters);

}
