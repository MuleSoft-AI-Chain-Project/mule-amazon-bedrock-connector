package org.mule.extension.aws.commons.internal.connection.provider;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.connectors.commons.template.connection.ConnectorConnection;
import org.mule.connectors.commons.template.connection.ConnectorConnectionProvider;
import org.mule.extension.aws.commons.internal.connection.provider.client.SdkHttpClientFactory;
import org.mule.extension.aws.commons.internal.connection.provider.parameters.CommonParameters;
import org.mule.extension.aws.commons.internal.connection.provider.parameters.ProxyParameterGroup;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.http.api.client.HttpClient;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

abstract class AWSConnectionProvider<T extends ConnectorConnection> extends ConnectorConnectionProvider<T>
    implements CachedConnectionProvider<T>, Initialisable, Startable, Stoppable {

  protected HttpClient httpClient;

  @ParameterGroup(name = "Connection")
  @Placement(order = 1)
  private CommonParameters commonParameters;

  @ParameterGroup(name = "Proxy")
  @Placement(order = 2)
  private ProxyParameterGroup proxyParameterGroup;

  protected final SdkHttpClientFactory sdkHttpClientFactory;

  public AWSConnectionProvider() {
    this.sdkHttpClientFactory = new SdkHttpClientFactory(proxyParameterGroup);
  }

  @Override
  public void start() throws MuleException {
    initialiseIfNeeded(getCommonParameters().getTlsContext());
  }

  @Override
  public void stop() {
    if (httpClient != null) {
      httpClient.stop();
    }
  }

  public void initialise() throws InitialisationException {
    TlsContextFactory tlsContextFactory = getCommonParameters().getTlsContext();
    if (tlsContextFactory instanceof Initialisable) {
      ((Initialisable) tlsContextFactory).initialise();
    }
  }

  public abstract T connect() throws ConnectionException;

  protected void onConnect(T connection) {}

  protected abstract AwsCredentialsProvider getAWSCredentialsProvider(CommonParameters commonParameters)
      throws ConnectionException;

  public CommonParameters getCommonParameters() {
    if (commonParameters == null) {
      return new CommonParameters();
    }
    return this.commonParameters;
  }

  public void setCommonParameters(CommonParameters commonParameters) {
    this.commonParameters = commonParameters;
  }

  protected SdkHttpClientFactory getSdkHttpClientFactory() {
    return sdkHttpClientFactory;
  }
}
