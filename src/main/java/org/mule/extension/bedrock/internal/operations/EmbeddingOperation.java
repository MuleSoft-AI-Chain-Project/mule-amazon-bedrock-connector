package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbedding;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbeddingDocument;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.EmbeddingService;
import org.mule.extension.bedrock.internal.service.EmbeddingServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.xml.sax.SAXException;

public class EmbeddingOperation extends BedrockOperation<EmbeddingService> {

  public EmbeddingOperation() {
    super(EmbeddingServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("EMBEDDING-generate-from-text")
  @Execution(ExecutionType.BLOCKING)
  public InputStream generateEmbedding(String prompt,
                                       @Config BedrockConfiguration config,
                                       @Connection BedrockConnection connection,
                                       @ParameterGroup(
                                           name = "Additional properties") BedrockParametersEmbedding awsBedrockParameters) {
    return newExecutionBuilder(config, connection)
        .execute(EmbeddingService::generateEmbeddings, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(awsBedrockParameters);
  }

  /**
   * Performs retrieval augmented generation based on files
   *
   * @throws TikaException
   * @throws SAXException
   * @throws IOException
   */

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("EMBEDDING-adhoc-query")
  @Execution(ExecutionType.BLOCKING)
  public InputStream ragEmbeddingTextScore(String prompt, String filePath,
                                           @Config BedrockConfiguration config,
                                           @Connection BedrockConnection connection,
                                           @ParameterGroup(
                                               name = "Additional properties") BedrockParametersEmbeddingDocument awsBedrockParameters)
      throws IOException, SAXException, TikaException {
    return newExecutionBuilder(config, connection)
        .execute(EmbeddingService::invokeAdhocRAG, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(filePath)
        .withParam(awsBedrockParameters);
  }
}
