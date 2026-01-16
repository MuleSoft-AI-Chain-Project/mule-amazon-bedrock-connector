package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockImageParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.ImageService;
import org.mule.extension.bedrock.internal.service.ImageServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class ImageOperation extends BedrockOperation<ImageService> {

  public ImageOperation() {
    super(ImageServiceImpl::new);
  }

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("IMAGE-generate")
  @Execution(ExecutionType.BLOCKING)
  public InputStream generateImage(String textToImage, String avoidInImage, String fullPathOutput,
                                   @Config BedrockConfiguration config,
                                   @Connection BedrockConnection connection,
                                   @ParameterGroup(
                                       name = "Additional properties") BedrockImageParameters awsBedrockParameters) {
    return newExecutionBuilder(config, connection)
        .execute(ImageService::invokeModel, BedrockModelFactory::createInputStream)
        .withParam(textToImage)
        .withParam(avoidInImage)
        .withParam(fullPathOutput)
        .withParam(awsBedrockParameters);
  }

}
