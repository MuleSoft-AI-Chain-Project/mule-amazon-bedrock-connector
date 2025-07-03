package org.mule.extension.mulechain.internal;

import org.mule.extension.mulechain.internal.proxy.ProxyConfig;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.extension.mulechain.internal.agents.AwsbedrockAgentsOperations;
import org.mule.extension.mulechain.internal.embeddings.AwsbedrockEmbeddingOperations;
import org.mule.extension.mulechain.internal.image.AwsbedrockImageModelOperations;
import org.mule.extension.mulechain.internal.memory.AwsbedrockMemoryOperations;
import org.mule.runtime.extension.api.annotation.Configuration;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple
 * operations since they represent something core from the extension.
 */
@Configuration(name="Config") 
@Operations({
          AwsbedrockOperations.class, 
          AwsbedrockImageModelOperations.class,
          AwsbedrockEmbeddingOperations.class,
          AwsbedrockMemoryOperations.class,
          AwsbedrockAgentsOperations.class
        })
public class AwsbedrockConfiguration {

  @Parameter
  private String awsAccessKeyId;
  
  @Parameter
  private String awsSecretAccessKey;


  @Parameter
  @Optional
  private String awsSessionToken;

  /**
   * Reusable configuration element for outbound connections through a proxy. A proxy element must define a host name and a port
   * attributes, and optionally can define a username and a password.
   */
  @Parameter
  @Optional
  @Summary("Reusable configuration element for outbound connections through a proxy")
  @Placement(tab = "Proxy")
  private ProxyConfig proxyConfig;

  public String getAwsAccessKeyId(){
    return awsAccessKeyId;
  }

  public String getAwsSecretAccessKey(){
    return awsSecretAccessKey;
  }

  public String getAwsSessionToken(){
    return awsSessionToken;
  }

  public ProxyConfig getProxyConfig() {
    return proxyConfig;
  }

}
