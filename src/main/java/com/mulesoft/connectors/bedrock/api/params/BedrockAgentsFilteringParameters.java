package com.mulesoft.connectors.bedrock.api.params;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.param.NullSafe;
import org.mule.sdk.api.annotation.param.Optional;

public class BedrockAgentsFilteringParameters {

  public enum RetrievalMetadataFilterType {
    AND_ALL, OR_ALL
  }

  public enum SearchType {
    HYBRID, SEMANTIC
  }

  public enum RerankingSelectionMode {
    SELECTIVE, ALL
  }

  public static class RerankingConfiguration {

    @Parameter
    @Optional
    private String rerankingType;

    @Parameter
    @Optional
    private String modelArn;

    @Parameter
    @Optional
    private Integer numberOfRerankedResults;

    @Parameter
    @Optional
    private RerankingSelectionMode selectionMode;

    @Parameter
    @Optional
    @NullSafe
    private List<String> fieldsToExclude;

    @Parameter
    @Optional
    @NullSafe
    private List<String> fieldsToInclude;

    @Parameter
    @Optional
    @NullSafe
    private Map<String, String> additionalModelRequestFields;

    public RerankingConfiguration() {
      // Default constructor intentionally empty.
      // Required for framework/deserialization/reflection-based instantiation.
    }

    public String getRerankingType() {
      return rerankingType;
    }

    public void setRerankingType(String rerankingType) {
      this.rerankingType = rerankingType;
    }

    public String getModelArn() {
      return modelArn;
    }

    public void setModelArn(String modelArn) {
      this.modelArn = modelArn;
    }

    public Integer getNumberOfRerankedResults() {
      return numberOfRerankedResults;
    }

    public void setNumberOfRerankedResults(Integer numberOfRerankedResults) {
      this.numberOfRerankedResults = numberOfRerankedResults;
    }

    public RerankingSelectionMode getSelectionMode() {
      return selectionMode;
    }

    public void setSelectionMode(RerankingSelectionMode selectionMode) {
      this.selectionMode = selectionMode;
    }

    public List<String> getFieldsToExclude() {
      return fieldsToExclude;
    }

    public void setFieldsToExclude(List<String> fieldsToExclude) {
      this.fieldsToExclude = fieldsToExclude;
    }

    public List<String> getFieldsToInclude() {
      return fieldsToInclude;
    }

    public void setFieldsToInclude(List<String> fieldsToInclude) {
      this.fieldsToInclude = fieldsToInclude;
    }

    public Map<String, String> getAdditionalModelRequestFields() {
      return additionalModelRequestFields;
    }

    public void setAdditionalModelRequestFields(Map<String, String> additionalModelRequestFields) {
      this.additionalModelRequestFields = additionalModelRequestFields;
    }
  }

  public static class KnowledgeBaseConfig {

    @Parameter
    @Optional
    private String knowledgeBaseId;

    @Parameter
    @Optional
    private Integer numberOfResults;

    @Parameter
    @Optional
    private SearchType overrideSearchType;

    @Parameter
    @Optional
    private RetrievalMetadataFilterType retrievalMetadataFilterType;

    @Parameter
    @Optional
    @NullSafe
    private Map<String, String> metadataFilters;

    @Parameter
    @Optional
    private RerankingConfiguration rerankingConfiguration;

    public KnowledgeBaseConfig() {}

    public KnowledgeBaseConfig(String knowledgeBaseId, Integer numberOfResults, SearchType overrideSearchType,
                               RetrievalMetadataFilterType retrievalMetadataFilterType, Map<String, String> metadataFilters) {
      this.knowledgeBaseId = knowledgeBaseId;
      this.numberOfResults = numberOfResults;
      this.overrideSearchType = overrideSearchType;
      this.retrievalMetadataFilterType = retrievalMetadataFilterType;
      this.metadataFilters = metadataFilters;
    }

    public String getKnowledgeBaseId() {
      return knowledgeBaseId;
    }

    public Integer getNumberOfResults() {
      return numberOfResults;
    }

    public SearchType getOverrideSearchType() {
      return overrideSearchType;
    }

    public RetrievalMetadataFilterType getRetrievalMetadataFilterType() {
      return retrievalMetadataFilterType;
    }

    public Map<String, String> getMetadataFilters() {
      return metadataFilters;
    }

    public RerankingConfiguration getRerankingConfiguration() {
      return rerankingConfiguration;
    }

    public void setRerankingConfiguration(RerankingConfiguration rerankingConfiguration) {
      this.rerankingConfiguration = rerankingConfiguration;
    }
  }

  @Parameter
  @Optional
  private String knowledgeBaseId;

  @Parameter
  @Optional
  private Integer numberOfResults;

  @Parameter
  @Optional
  private SearchType overrideSearchType;

  @Parameter
  @Optional
  private RetrievalMetadataFilterType retrievalMetadataFilterType;

  @Parameter
  @Optional
  @NullSafe
  private Map<String, String> metadataFilters;

  public String getKnowledgeBaseId() {
    return knowledgeBaseId;
  }

  public Integer getNumberOfResults() {
    return numberOfResults;
  }

  public SearchType getOverrideSearchType() {
    return overrideSearchType;
  }

  public RetrievalMetadataFilterType getRetrievalMetadataFilterType() {
    return retrievalMetadataFilterType;
  }

  public Map<String, String> getMetadataFilters() {
    return metadataFilters;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockAgentsFilteringParameters that))
      return false;
    return Objects.equals(knowledgeBaseId, that.knowledgeBaseId) && Objects.equals(numberOfResults, that.numberOfResults)
        && overrideSearchType == that.overrideSearchType && retrievalMetadataFilterType == that.retrievalMetadataFilterType
        && Objects.equals(metadataFilters, that.metadataFilters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(knowledgeBaseId, numberOfResults, overrideSearchType, retrievalMetadataFilterType, metadataFilters);
  }
}
