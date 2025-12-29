# AWS Bedrock Overview

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mulesoft-ai-chain-project/mule4-amazon-bedrock-connector)](https://central.sonatype.com/artifact/io.github.mulesoft-ai-chain-project/mule4-amazon-bedrock-connector/overview)

[**AWS Bedrock**](https://aws.amazon.com/bedrock) is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies including AI21 Labs, Anthropic, Cohere, Meta, Stability AI, and Amazon, along with a broad set of capabilities that you need to build generative AI applications, simplifying development while maintaining privacy and security.
- Amazon Bedrock offers a choice of leading FM’s through a Single API
- Using Amazon Bedrock, you can easily experiment with and evaluate top FMs for your use case, privately customize them with your data using techniques such as fine-tuning and Retrieval Augmented Generation (RAG)
- Build agents that execute multistep tasks using your enterprise systems and data sources
- Since Amazon Bedrock is serverless, you don't have to manage any infrastructure, and you can securely integrate and deploy generative AI capabilities into your applications using the AWS services

## Why MAC AWS Bedrock Connector?

AWS Bedrock provides an unified AI Platform to design, build and manage autonomous agents and the needed architecture. While AWS Bedrock is very strong in connecting multiple LLM providers, the way to interact varies from LLM to LLM. The MAC AWS Bedrock Connector provides the ability to connect to all supported LLMs through an unification layer.

### Installation (using maven central dependency)

```xml
<dependency>
   <groupId>io.github.mulesoft-ai-chain-project</groupId>
   <artifactId>mule4-amazon-bedrock-connector</artifactId>
   <version>{version}</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/aws-bedrock/getting-started) the connector into your local maven repository.
Then add the following dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule4-amazon-bedrock-connector</artifactId>
    <version>{version}</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Installation into private Anypoint Exchange

You can also make this connector available as an asset in your Anyooint Exchange.

This process will require you to build the connector as above, but additionally you will need
to make some changes to the `pom.xml`.  For this reason, we recommend you fork the repository.

Then, follow the MuleSoft [documentation](https://docs.mulesoft.com/exchange/to-publish-assets-maven) to modify and publish the asset.

## Learn more at [mac-project.ai](https://mac-project.ai/docs/aws-bedrock/connector-overview)

## Features

### Multiple Knowledge Base Support

The AWS Bedrock InvokeAgent API supports querying multiple knowledge bases in a single agent invocation. The MAC AWS Bedrock Connector provides flexible configuration options to leverage this capability:

- **Single Knowledge Base (Legacy)**: Configure a single knowledge base with its settings through individual parameters
- **Multiple Knowledge Bases**: Pass a list of knowledge base configurations, each with independent settings for:
  - Knowledge Base ID
  - Number of results to retrieve
  - Search type (Hybrid or Semantic)
  - Metadata filters and filter types (AND_ALL/OR_ALL)

**Why Two Options?**

To maintain **full backwards compatibility** with existing Mule flows while enabling the new multi-KB capability. Existing integrations continue to work without modification, while new implementations can leverage multiple knowledge bases when needed.

**Precedence**: When both single and multiple KB configurations are provided, the multiple KB list takes precedence, and a warning is logged about the ignored legacy parameters.

This design allows you to:
- Query across multiple knowledge bases simultaneously
- Apply different retrieval configurations per knowledge base
- Maintain existing flows without breaking changes

## Version History

### Version 0.5.6

**New Feature: Reranking Configuration for Knowledge Base Queries**

Added support for reranking configuration in Agent Chat and Agent Chat Streamed operations. This feature allows you to improve the relevance of query responses by reranking retrieved results using Amazon Bedrock reranker models.

**Reranking Configuration Parameters:**

- **Reranking Type**: Type of reranking configuration (defaults to "BEDROCK") - *Optional*
- **Model ARN**: The Amazon Resource Name (ARN) of the foundation model to use for reranking - **Required**
- **Number of Reranked Results**: The number of results to return after reranking - *Optional*
- **Selection Mode**: How to consider metadata when reranking - *Optional*:
  - `ALL`: Consider all metadata fields
  - `SELECTIVE`: Consider only selected metadata fields
- **Fields to Exclude**: (When SELECTIVE mode) List of metadata field names to exclude from consideration - *Optional*
- **Fields to Include**: (When SELECTIVE mode) List of metadata field names to include in consideration (mutually exclusive with fieldsToExclude) - *Optional*
- **Additional Model Request Fields**: Optional map of additional fields to include in the model request during reranking - *Optional*

**Important:** The `modelArn` field is **required** for reranking configuration. If `modelArn` is not provided, the reranking configuration will be skipped entirely and the request will proceed without reranking. All other parameters are optional and will only be applied if provided.

**Usage:**

The reranking configuration is part of the knowledge base configuration and can be specified for each knowledge base when using multiple knowledge bases. This allows you to apply different reranking strategies per knowledge base.

**Benefits:**

- Improve search result relevance by reranking retrieved documents
- Fine-tune reranking behavior using selective metadata filtering
- Configure reranking independently for each knowledge base in multi-KB scenarios
- Leverage Amazon Bedrock's reranker models for better search quality

For more information, see the [AWS Bedrock documentation on reranking](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_agent-runtime_InvokeAgent.html).
