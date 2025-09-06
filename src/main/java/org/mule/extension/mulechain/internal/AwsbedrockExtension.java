package org.mule.extension.mulechain.internal;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;

import org.mule.extension.mulechain.internal.proxy.DefaultProxyConfig;
import org.mule.extension.mulechain.internal.proxy.ProxyConfig;
import org.mule.runtime.extension.api.annotation.*;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.JavaVersionSupport;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations and
 * sources are going to be declared.
 */
@Xml(prefix = "mac-bedrock")
@Extension(name = "Amazon Bedrock")
@Configurations(AwsbedrockConfiguration.class)
@SubTypeMapping(baseType = ProxyConfig.class, subTypes = {DefaultProxyConfig.class})
@ErrorTypes(BedrockError.class)
@Operations(AwsbedrockOperations.class)
@Export(classes = {ProxyConfig.class})
@JavaVersionSupport({JAVA_17})
public class AwsbedrockExtension {

}
