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

## Version History

### Version 0.6.0

**New Feature: Retry Mechanism with Exponential Backoff**

Added comprehensive retry support for agent chat operations (both streaming and non-streaming) with configurable exponential backoff.

**Retry Configuration:**
- **Enable Retry**: Toggle to enable/disable retry mechanism (default: false)
- **Max Retries**: Maximum number of retry attempts (default: 3)
- **Retry Backoff**: Base backoff delay in milliseconds for exponential backoff (default: 1000ms)

**How Retry Works:**
- Retries only occur on retryable exceptions (timeouts, network errors, connection issues)
- Exponential backoff: Wait times double after each attempt (1s → 2s → 4s → 8s...)
- For streaming operations: Only retries if no chunks have been received yet (prevents duplicate data)
- For non-streaming operations: Can always retry on retryable exceptions

**Retryable Exceptions:**
- `TimeoutException` and timeout-related errors
- `SdkClientException` with timeout/network/connection messages
- `CompletionException` wrapping retryable exceptions
- Non-retryable: Validation errors, authentication errors, errors after chunks received

**New Feature: Operation-Level Timeout Override**

Added operation-level timeout configuration in the "Response" tab for agent operations.

**Response Tab Parameters:**
- **Request Timeout**: Operation-level timeout that overrides connector-level timeout if provided (optional)
- **Request Timeout Unit**: Unit for the request timeout (optional)
- **Enable Retry**: Enable retry mechanism (moved from "Additional properties")
- **Max Retries**: Maximum retry attempts (moved from "Additional properties")
- **Retry Backoff**: Base backoff delay in milliseconds (moved from "Additional properties")

**How Timeout Override Works:**
- If operation-level timeout is provided → Uses operation-level timeout, creates separate cached client
- If not provided → Uses connector-level timeout (default behavior), shares cached client
- Connector-level timeout remains unchanged and serves as default for all operations

**UI Changes:**
- New "Response" tab added to agent chat operations (appears below "MIME Type" tab)
- Retry parameters moved from "Additional properties" to "Response" tab
- "Additional properties" now only contains `modelName` and `region`

**Behavior Change: Streaming Agent Operation Session Start Event**

**Improved Timing of `session-start` Event in Streaming Agent Operations**

The `session-start` SSE event in the streaming agent operation (`AGENT-chat-streaming-SSE`) now fires right before the first chunk from Bedrock is received, rather than immediately when the stream is initiated.

**What Changed:**
- Previously, `session-start` was sent immediately when the streaming operation started, before the Bedrock API was even invoked
- Now, `session-start` is sent only when Bedrock begins responding with the first chunk

**Benefits:**
- Per-operation timeout control for fine-grained configuration
- Retry mechanism improves resilience to transient failures
- Exponential backoff prevents overwhelming the service
- Better organization of response-related settings
- More accurate timing: The `session-start` event now indicates that Bedrock has actually started responding, not just that the request was initiated
- Better alignment with actual Bedrock response timing
- If Bedrock completes without sending chunks, `session-start` will not be sent (since there are no chunks to precede)

**Impact:**
- **No breaking changes**: All parameters are optional with sensible defaults
- Existing flows continue to work without modification
- New "Response" tab provides better organization of timeout and retry settings
- Retry is opt-in (disabled by default)
- This is a **behavioral change** for `session-start` event timing that may affect clients expecting `session-start` immediately upon stream initiation
- Clients should now wait for the first chunk to receive the `session-start` event
- The event still contains the same metadata (sessionId, agentId, agentAlias, prompt, timestamp, status)

**Security Updates: Dependency Upgrades**

This version includes critical security updates to address vulnerabilities in core dependencies:

**Netty Framework Upgrade (4.1.x → 4.2.9.Final)**
- Upgraded all Netty modules to version **4.2.9.Final** (from 4.1.124.Final/4.1.125.Final)
- **Fixed CVE-2025-67735**: CRLF injection vulnerability in `HttpRequestEncoder` that could enable HTTP request smuggling
- All Netty modules upgraded together to ensure compatibility:
- `netty-codec-http`, `netty-codec-http2`, `netty-codec`, `netty-transport`, `netty-common`, `netty-buffer`, `netty-handler`, `netty-resolver`, and native transport modules
- **Note**: Netty 4.2.x introduces TLS hostname verification by default and uses adaptive memory allocator. See [Netty 4.2 Migration Guide](https://netty.io/wiki/netty-4.2-migration-guide.html) for details.

**Apache Tika Upgrade (2.9.4 → 3.2.3)**
- Upgraded all Tika modules to version **3.2.3** (from 2.9.4)
- **Fixed CVE-2025-66516**: Critical XXE (XML External Entity) vulnerability in `tika-core` that could lead to file disclosure, SSRF, and potential remote code execution
- All Tika modules upgraded together:
- `tika-core`, `tika-parser-pdf-module`, `tika-parsers`
- **Note**: Tika 3.x requires Java 11+ (compatible with Java 17 used by this connector)

**Impact:**
- These upgrades address critical security vulnerabilities and improve overall security posture
- No breaking changes expected for typical usage patterns
- All upgrades maintain backward compatibility with existing functionality

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

**Bug Fix: Dynamic Region Parameter Support**

Fixed an issue where AWS Bedrock client instances were cached using static keys without including the region parameter. This caused all operations to reuse the first client created, regardless of the region specified in subsequent calls. The region parameter now works correctly at runtime, with each region getting its own cached client instance.

### Version 0.5.5

**New Feature: Multiple Knowledge Base Support**

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
