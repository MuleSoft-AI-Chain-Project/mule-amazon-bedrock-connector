package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockParamRegion;
import org.mule.extension.bedrock.api.params.BedrockParamsModelDetails;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.FoundationalService;
import org.mule.extension.bedrock.internal.service.FoundationalServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class FoundationalModelOperations extends BedrockOperation<FoundationalService> {

  public FoundationalModelOperations() {
    super(FoundationalServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("FOUNDATIONAL-model-details")
  public InputStream getFoundationModelDetails(@Config BedrockConfiguration config,
                                               @Connection BedrockConnection connection,
                                               @ParameterGroup(
                                                   name = "Additional properties") BedrockParamsModelDetails bedrockParamsModelDetails) {
    return newExecutionBuilder(config, connection)
        .execute(FoundationalService::getFoundationModel, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsModelDetails);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("FOUNDATIONAL-models-list")
  public InputStream listFoundationModels(@Config BedrockConfiguration config,
                                          @Connection BedrockConnection connection,
                                          @ParameterGroup(
                                              name = "Additional properties") BedrockParamRegion bedrockParamsRegion) {
    return newExecutionBuilder(config, connection)
        .execute(FoundationalService::listFoundationModels, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsRegion);
  }
}
