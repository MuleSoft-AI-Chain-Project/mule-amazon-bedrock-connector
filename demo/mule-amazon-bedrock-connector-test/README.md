# MuleSoft Amazon Bedrock Connector Test App

> **Note:** This demo application is still a work in progress and some operations may be missing or incomplete.

## Introduction

This demo application will test the following operations:
- Chat-Answer-Prompt
- Agent-Prompt-Template
- Sentiment-Analyse
- Generate Image

## Prerequisites

- MuleSoft Amazon Bedrock Connector

## Configure your application
Configure /src/main/resources/llm.properties with the following properties:
aws_acces_key={YOUR AWS ACCESS KEY}
aws_secret_key={YOUR AWS ACCESS KEY}
llm_image_gen_model_name={model that supports image generation. ex: amazon.nova-canvas-v1:0}
llm_model_name={model that supports text generation. ex: anthropic.claude-3-7-sonnet-20250219-v1:0}
llm_region={region, ex: us-east-1}


## How to test the different operations?
Use the attached Postman Collection - Amazon-Bedrock.postman_collection.json