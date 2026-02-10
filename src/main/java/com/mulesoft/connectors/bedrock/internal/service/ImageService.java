package com.mulesoft.connectors.bedrock.internal.service;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockImageParameters;
import org.mule.connectors.commons.template.service.ConnectorService;

public interface ImageService extends ConnectorService {

  String invokeModel(String prompt, String avoidInImage, String fullPath,
                     BedrockImageParameters awsBedrockParameters);

}
