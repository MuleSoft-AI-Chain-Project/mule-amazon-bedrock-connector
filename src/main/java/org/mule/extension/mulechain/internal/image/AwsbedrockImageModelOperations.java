package org.mule.extension.mulechain.internal.image;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.helpers.AwsbedrockImagePayloadHelper;

/**
 * This class is a container for operations, every public method in this class
 * will be taken as an extension operation.
 */
public class AwsbedrockImageModelOperations {

  /**
   * Generates an image based on text.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("IMAGE-generate")
  public InputStream generateImage(String TextToImage, String AvoidInImage, String fullPathOutput,
      @Config AwsbedrockConfiguration configuration,
      @ParameterGroup(name = "Additional properties") AwsbedrockImageParameters awsBedrockParameters) {
    String response = AwsbedrockImagePayloadHelper.invokeModel(TextToImage, AvoidInImage, fullPathOutput, configuration,
        awsBedrockParameters);
    return toInputStream(response, StandardCharsets.UTF_8);
  }

}
