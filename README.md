# AWS Bedrock Overview
![Maven Central](https://img.shields.io/maven-central/v/cloud.anypoint/mule-amazon-bedrock-connector)

[**AWS Bedrock**](https://aws.amazon.com/bedrock) is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies including AI21 Labs, Anthropic, Cohere, Meta, Stability AI, and Amazon, along with a broad set of capabilities that you need to build generative AI applications, simplifying development while maintaining privacy and security.
- Amazon Bedrock offers a choice of leading FM’s through a Single API
- Using Amazon Bedrock, you can easily experiment with and evaluate top FMs for your use case, privately customize them with your data using techniques such as fine-tuning and Retrieval Augmented Generation (RAG)
- Build agents that execute multistep tasks using your enterprise systems and data sources
- Since Amazon Bedrock is serverless, you don't have to manage any infrastructure, and you can securely integrate and deploy generative AI capabilities into your applications using the AWS services

## Why MAC AWS Bedrock Connector?

AWS Bedrock provides an unified AI Platform to design, build and manage autonomous agents and the needed architecture. While AWS Bedrock is very strong in connecting multiple LLM providers, the way to interact varies from LLM to LLM. The MAC AWS Bedrock Connector provides the ability to connect to all supported LLMs through an unification layer. 

### Installation (using Cloud.Anypoint Dependency)

```xml
<dependency>
   <groupId>cloud.anypoint</groupId>
   <artifactId>mule-amazon-bedrock-connector</artifactId>
   <version>0.2.0</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/aws-bedrock/getting-started) the connector into your local maven repository.
Then add the following dependency to your application's `pom.xml`:

```xml
<dependency>
   <groupId>com.mule.mulechain</groupId>
   <artifactId>mulechain-bedrock</artifactId>
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
