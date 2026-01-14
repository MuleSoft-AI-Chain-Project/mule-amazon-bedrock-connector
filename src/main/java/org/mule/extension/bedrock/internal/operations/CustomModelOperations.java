package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockParamRegion;
import org.mule.extension.bedrock.api.params.BedrockParamsModelDetails;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.CustomModelService;
import org.mule.extension.bedrock.internal.service.CustomModelServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class CustomModelOperations extends BedrockOperation<CustomModelService> {

  public CustomModelOperations() {
    super(CustomModelServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("CUSTOM-model-details")
  public InputStream getCustomModelDetails(@Config BedrockConfiguration config,
                                           @Connection BedrockConnection connection,
                                           @ParameterGroup(
                                               name = "Additional properties") BedrockParamsModelDetails bedrockParamsModelDetails) {
    return newExecutionBuilder(config, connection)
        .execute(CustomModelService::getCustomModelByModelID, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsModelDetails);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("CUSTOM-models-list")
  public InputStream listCustomModels(@Config BedrockConfiguration config,
                                      @Connection BedrockConnection connection,
                                      @ParameterGroup(name = "Additional properties") BedrockParamRegion bedrockParamsRegion) {
    return newExecutionBuilder(config, connection)
        .execute(CustomModelService::listCustomModels, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsRegion);
  }
}
