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
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class FoundationalModelOperations extends BedrockOperation<FoundationalService> {

  public FoundationalModelOperations() {
    super(FoundationalServiceImpl::new);
  }

  /**
   * Retrieves detailed information about a specific foundation model available in Amazon Bedrock.
   *
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param bedrockParamsModelDetails Parameters including model identifier and region.
   * @return InputStream containing the JSON response with model details including model ID, name, provider, input/output
   *         modalities, and supported inference types.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("FOUNDATIONAL-model-details")
  @Summary("Retrieve details of a specific foundation model")
  public InputStream getFoundationModelDetails(@Config BedrockConfiguration config,
                                               @Connection BedrockConnection connection,
                                               @ParameterGroup(
                                                   name = "Additional properties") BedrockParamsModelDetails bedrockParamsModelDetails) {
    return newExecutionBuilder(config, connection)
        .execute(FoundationalService::getFoundationModel, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsModelDetails);
  }

  /**
   * Lists all foundation models available in Amazon Bedrock for the specified region.
   *
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param bedrockParamsRegion Parameters including region to list models for.
   * @return InputStream containing the JSON response with list of available foundation models including model IDs, names,
   *         providers, and capabilities.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("FOUNDATIONAL-models-list")
  @Summary("List available Bedrock foundation models")
  public InputStream listFoundationModels(@Config BedrockConfiguration config,
                                          @Connection BedrockConnection connection,
                                          @ParameterGroup(
                                              name = "Additional properties") BedrockParamRegion bedrockParamsRegion) {
    return newExecutionBuilder(config, connection)
        .execute(FoundationalService::listFoundationModels, BedrockModelFactory::createInputStream)
        .withParam(bedrockParamsRegion);
  }
}
