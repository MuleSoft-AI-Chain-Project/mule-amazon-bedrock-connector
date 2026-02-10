package com.mulesoft.connectors.bedrock.internal.service;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbeddingDocument;
import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.xml.sax.SAXException;

public interface EmbeddingService extends ConnectorService {

  String generateEmbeddings(String prompt, BedrockParametersEmbedding bedrockEmbeddingParameters);

  String invokeAdhocRAG(String prompt, String filePath, BedrockParametersEmbeddingDocument bedrockEmbeddingParameters)
      throws IOException, SAXException, TikaException;

}
