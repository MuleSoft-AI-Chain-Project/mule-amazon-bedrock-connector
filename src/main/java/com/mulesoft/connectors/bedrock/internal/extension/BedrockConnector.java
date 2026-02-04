package com.mulesoft.connectors.bedrock.internal.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.sdk.api.annotation.JavaVersionSupport;


@Extension(name = "Amazon Bedrock", category = Category.SELECT)
@RequiresEnterpriseLicense(allowEvaluationLicense = true)
@Configurations(BedrockConfiguration.class)
@Xml(prefix = "ms-bedrock")
@ErrorTypes(BedrockErrorType.class)
@JavaVersionSupport({JAVA_17})

public class BedrockConnector {

}
