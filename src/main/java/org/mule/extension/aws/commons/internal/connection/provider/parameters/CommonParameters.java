package org.mule.extension.aws.commons.internal.connection.provider.parameters;

import java.util.concurrent.TimeUnit;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.mule.sdk.api.annotation.semantics.security.Password;
import org.mule.sdk.api.annotation.semantics.security.Username;

public class CommonParameters {

  @Parameter
  @Optional(
      defaultValue = "50")
  @Placement(
      tab = "Advanced",
      order = 1)
  @DisplayName("Connection Timeout")
  @ExcludeFromConnectivitySchema
  @Summary("The amount of time to wait (in milliseconds) when initially establishing a connection before the connector gives up and times out. A value of 0 means infinity and is not recommended.")
  private Integer connectionTimeout;
  @Parameter
  @Optional(
      defaultValue = "SECONDS")
  @Placement(
      tab = "Advanced",
      order = 2)
  @ExcludeFromConnectivitySchema
  @Summary("Time unit used in the connection timeout configurations.")
  private TimeUnit connectionTimeoutUnit;
  @Parameter
  @Optional(defaultValue = "50")
  @Placement(
      tab = "Advanced",
      order = 3)
  @ExcludeFromConnectivitySchema
  @Summary("Sets the maximum number of allowed open HTTP connections.")
  private Integer maxConnections;
  @Parameter
  @Optional(
      defaultValue = "50")
  @Placement(
      tab = "Advanced",
      order = 4)
  @DisplayName("Socket Timeout")
  @ExcludeFromConnectivitySchema
  @Summary("The amount of time to wait (in milliseconds) for data to be transferred over an established, open connection before the connection times out. A value of 0 means infinity and is not recommended.")
  private Integer socketTimeout;
  @Parameter
  @Optional(
      defaultValue = "SECONDS")
  @Placement(
      tab = "Advanced",
      order = 5)
  @ExcludeFromConnectivitySchema
  @Summary("Time unit used in the socket timeout configurations.")
  private TimeUnit socketTimeoutUnit;
  @Parameter
  @DisplayName("Access Key")
  @Placement(
      order = 4)
  @Summary("The access key provided by Amazon.")
  @Username
  private String accessKey;
  @Parameter
  @DisplayName("Secret Key")
  @Placement(
      order = 5)
  @Summary("The secret key provided by Amazon.")
  @Password
  private String secretKey;
  @OfValues(RegionValuesProvider.class)
  @Optional(
      defaultValue = "us-east-1")
  @Parameter
  @DisplayName("Region Endpoint")
  @Placement(
      order = 8)
  @Summary("Set the topic region endpoint.")
  private String region;
  @Parameter
  @DisplayName("Try Default AWSCredentials Provider Chain")
  @Placement(
      tab = "Advanced",
      order = 6)
  @Optional
  @ExcludeFromConnectivitySchema
  @Summary("Set this field to true to obtain credentials from the AWS environment, See: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html")
  private boolean tryDefaultAWSCredentialsProviderChain;
  @Parameter
  @DisplayName("Custom Service Endpoint")
  @Optional
  @Placement(
      tab = "Advanced",
      order = 7)
  @ExcludeFromConnectivitySchema
  @Summary("Sets a custom service endpoint. Useful when a non-standard service endpoint is required, such as a VPC endpoint.")
  private String customServiceEndpoint;
  @Expression(ExpressionSupport.NOT_SUPPORTED)
  @Placement(
      tab = "Security",
      order = 9)
  @Parameter
  @Optional
  @DisplayName("TLS Configuration")
  private TlsContextFactory tlsContext;

  public CommonParameters() {}

  public Integer getConnectionTimeout() {
    return Math.toIntExact(this.connectionTimeoutUnit.toMillis((long) this.connectionTimeout));
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public Integer getSocketTimeout() {
    return Math.toIntExact(this.socketTimeoutUnit.toMillis((long) this.socketTimeout));
  }

  public void setSocketTimeout(Integer socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public String getAccessKey() {
    return this.accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return this.secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public boolean isTryDefaultAWSCredentialsProviderChain() {
    return this.tryDefaultAWSCredentialsProviderChain;
  }

  public void setTryDefaultAWSCredentialsProviderChain(boolean tryDefaultAWSCredentialsProviderChain) {
    this.tryDefaultAWSCredentialsProviderChain = tryDefaultAWSCredentialsProviderChain;
  }

  public Integer getMaxConnections() {
    return this.maxConnections;
  }

  public void setMaxConnections(Integer maxConnections) {
    this.maxConnections = maxConnections;
  }

  public String getRegion() {
    if (this.region != null && !this.region.isEmpty()) {
      return this.region.toLowerCase().replace("_", "-");
    } else {
      throw new IllegalArgumentException("The region is required.");
    }
  }

  public String getCustomServiceEndpoint() {
    return this.customServiceEndpoint;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public void setTlsContext(TlsContextFactory tlsContext) {
    this.tlsContext = tlsContext;
  }

  public TlsContextFactory getTlsContext() {
    return this.tlsContext;
  }
}
