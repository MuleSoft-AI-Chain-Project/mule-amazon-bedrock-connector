package com.mulesoft.connectors.bedrock.internal.operation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import com.mulesoft.connectors.bedrock.api.params.BedrockImageParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.provider.BedrockErrorsProvider;
import com.mulesoft.connectors.bedrock.internal.service.ImageService;
import com.mulesoft.connectors.bedrock.internal.service.ImageServiceImpl;
import com.mulesoft.connectors.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class ImageOperation extends BedrockOperation<ImageService> {

  public ImageOperation() {
    super(ImageServiceImpl::new);
  }

  /**
   * Generates an image from a text prompt using Amazon Bedrock image generation models (e.g., Stable Diffusion, Amazon Titan
   * Image).
   *
   * @param textToImage The text prompt describing the image to generate.
   * @param avoidInImage Text describing what should be avoided in the generated image (negative prompt).
   * @param fullPathOutput File system path where the generated image should be saved (optional).
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param awsBedrockParameters Additional properties including model name, region, image dimensions, number of images, and
   *        quality settings.
   * @return InputStream containing the JSON response with generated image data (base64 encoded) and metadata.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("IMAGE-generate")
  @Execution(ExecutionType.BLOCKING)
  @Summary("Generate images using Bedrock-supported models")
  public InputStream generateImage(@Config BedrockConfiguration config,
                                   @Connection BedrockConnection connection,
                                   @ParameterGroup(
                                       name = "Additional properties") BedrockImageParameters awsBedrockParameters,
                                   String textToImage, String avoidInImage, String fullPathOutput) {
    return newExecutionBuilder(config, connection)
        .execute(ImageService::invokeModel, BedrockModelFactory::createInputStream)
        .withParam(textToImage)
        .withParam(avoidInImage)
        .withParam(fullPathOutput)
        .withParam(awsBedrockParameters);
  }

}
