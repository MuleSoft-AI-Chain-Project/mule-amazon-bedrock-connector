package org.mule.extension.bedrock.internal.service;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParamRegion;
import org.mule.extension.bedrock.api.params.BedrockParamsModelDetails;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrock.model.CustomModelSummary;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListCustomModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ValidationException;

public class CustomModelServiceImpl extends BedrockServiceImpl implements CustomModelService {

  private static final Logger logger = LoggerFactory.getLogger(CustomModelServiceImpl.class);

  public CustomModelServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String getCustomModelByModelID(BedrockParamsModelDetails bedrockParamsModelDetails) {
    try {
      GetCustomModelResponse response = getConnection().getCustomModel(
                                                                       GetCustomModelRequest.builder()
                                                                           .modelIdentifier(bedrockParamsModelDetails
                                                                               .getModelName())
                                                                           .build());
      JSONObject jsonModel = new JSONObject();
      jsonModel.put("modelArn", response.modelArn());
      jsonModel.put("modelName", response.modelName());
      jsonModel.put("jobName", response.jobName());
      jsonModel.put("jobArn", response.jobArn());
      jsonModel.put("customizationTypeAsString", response.customizationTypeAsString());
      jsonModel.put("baseModelArn", response.baseModelArn());
      jsonModel.put("hyperParameters", response.hyperParameters());
      jsonModel.put("hasHyperParameters", response.hasHyperParameters());
      jsonModel.put("hasValidationMetrics", response.hasValidationMetrics());

      return jsonModel.toString();

    } catch (ValidationException e) {
      throw new IllegalArgumentException(e.getMessage());
    } catch (SdkException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public String listCustomModels(BedrockParamRegion bedrockParamRegion) {
    try {
      ListCustomModelsResponse response = getConnection().listCustomModels();

      List<CustomModelSummary> models = response.modelSummaries();

      logger.info(response.toString());
      logger.info(models.toString());

      JSONArray modelsArray = new JSONArray();

      if (models.isEmpty()) {
        logger.info("No available foundation models.");
      } else {
        for (CustomModelSummary model : models) {
          // Create a JSONObject for each model and add to JSONArray
          JSONObject modelJson = new JSONObject();
          modelJson.put("provider", model.modelName());
          modelJson.put("modelArn", model.modelArn());
          modelJson.put("baseModelArn", model.baseModelArn());
          modelJson.put("baseModelName", model.baseModelName());
          modelJson.put("customizationType", model.customizationTypeAsString());

          modelsArray.put(modelJson);
        }
      }

      return modelsArray.toString();

    } catch (SdkClientException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
