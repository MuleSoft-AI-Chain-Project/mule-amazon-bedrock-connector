package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.SentimentService;
import org.mule.extension.bedrock.internal.service.SentimentServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class SentimentOperations extends BedrockOperation<SentimentService> {

  public SentimentOperations() {
    super(SentimentServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("SENTIMENT-analyze")
  public InputStream sentimentAnalysis(@Config BedrockConfiguration config,
                                       @Connection BedrockConnection connection,
                                       String TextToAnalyze,
                                       @ParameterGroup(name = "Additional properties") BedrockParameters bedrockParameters) {

    return newExecutionBuilder(config, connection)
        .execute(SentimentService::extractSentiments, BedrockModelFactory::createInputStream)
        .withParam(TextToAnalyze)
        .withParam(bedrockParameters);
  }
}
