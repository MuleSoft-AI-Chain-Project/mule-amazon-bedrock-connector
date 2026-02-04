package com.mulesoft.connectors.bedrock.internal.connection.client;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.ProxyParameterGroup;
import org.mule.runtime.api.connection.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

public class SdkHttpClientFactory {

  private final ProxyParameterGroup proxyParameterGroup;

  private static final Logger LOGGER = LoggerFactory.getLogger(SdkHttpClientFactory.class);


  public SdkHttpClientFactory(ProxyParameterGroup proxyParameterGroup) {
    this.proxyParameterGroup = proxyParameterGroup;
  }

  public SdkHttpClient buildHttpClient(CommonParameters commonParameters) throws ConnectionException {
    ApacheHttpClient.Builder clientBuilder = ApacheHttpClient.builder();
    proxyConfiguration(clientBuilder);
    tlsConfiguration(clientBuilder, commonParameters);
    clientBuilder.connectionTimeout(Duration.ofMillis(commonParameters.getConnectionTimeout()));
    clientBuilder.socketTimeout(Duration.ofMillis(commonParameters.getSocketTimeout()));
    return clientBuilder.build();
  }

  private void tlsConfiguration(ApacheHttpClient.Builder clientBuilder, CommonParameters commonParameters)
      throws ConnectionException {
    if (commonParameters.getTlsContext() != null) {
      try {
        clientBuilder.socketFactory(new SSLConnectionSocketFactory(commonParameters.getTlsContext().createSslContext()));
      } catch (KeyManagementException | NoSuchAlgorithmException e) {
        LOGGER.info("SSL/TLS configuration error: " + e.getMessage());
        throw new ConnectionException("SSL/TLS configuration error", e);
      }
    }
  }

  private void tlsConfiguration(NettyNioAsyncHttpClient.Builder clientBuilder, CommonParameters commonParameters)
      throws ConnectionException {
    if (commonParameters.getTlsContext() != null) {
      try {
        String keyType = commonParameters.getTlsContext().getTrustStoreConfiguration().getType();
        String keyPath = commonParameters.getTlsContext().getTrustStoreConfiguration().getPath();
        String keyPassword = commonParameters.getTlsContext().getTrustStoreConfiguration().getPassword();
        KeyStore trustStore = KeyStore.getInstance(keyType);
        try (FileInputStream fis = new FileInputStream(keyPath)) {
          trustStore.load(fis, keyPassword != null ? keyPassword.toCharArray() : null);
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        clientBuilder.tlsTrustManagersProvider(trustManagerFactory::getTrustManagers);
      } catch (Exception e) {
        LOGGER.info("TLS configuration error: " + e.getMessage());
        throw new ConnectionException("TLS configuration error", e);
      }
    }
  }

  private void proxyConfiguration(ApacheHttpClient.Builder clientBuilder) {
    if (this.proxyParameterGroup != null && this.proxyParameterGroup.getProxyHost() != null &&
        this.proxyParameterGroup.getProxyPort() != null) {
      clientBuilder.proxyConfiguration(ProxyConfiguration.builder()
          .endpoint(URI.create(this.proxyParameterGroup.getProxyHost() + ":" + this.proxyParameterGroup.getProxyPort()))
          .username(this.proxyParameterGroup.getProxyUsername())
          .password(this.proxyParameterGroup.getProxyPassword())
          .ntlmWorkstation(this.proxyParameterGroup.getProxyWorkstation())
          .ntlmDomain(this.proxyParameterGroup.getProxyDomain())
          .build());
    }
  }

  private void proxyConfiguration(NettyNioAsyncHttpClient.Builder clientBuilder) {
    if (this.proxyParameterGroup != null && this.proxyParameterGroup.getProxyHost() != null &&
        this.proxyParameterGroup.getProxyPort() != null) {
      clientBuilder.proxyConfiguration(software.amazon.awssdk.http.nio.netty.ProxyConfiguration.builder()
          .host(this.proxyParameterGroup.getProxyHost())
          .port(this.proxyParameterGroup.getProxyPort())
          .username(this.proxyParameterGroup.getProxyUsername())
          .password(this.proxyParameterGroup.getProxyPassword())
          .build());
    }
  }

  public SdkAsyncHttpClient buildHttpAsyncClient(CommonParameters commonParameters) throws ConnectionException {
    return buildHttpAsyncClient(commonParameters, null);
  }

  /**
   * Builds an async HTTP client with optional read/connection timeout override. When {@code readTimeoutMsOverride} is non-null,
   * it is used for both connection and read timeout (so clients with different timeouts can be cached separately). Otherwise
   * connection-level timeouts from {@code commonParameters} are used.
   */
  public SdkAsyncHttpClient buildHttpAsyncClient(CommonParameters commonParameters, Long readTimeoutMsOverride)
      throws ConnectionException {
    NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();
    proxyConfiguration(httpClientBuilder);
    tlsConfiguration(httpClientBuilder, commonParameters);
    long timeoutMs = readTimeoutMsOverride != null ? readTimeoutMsOverride : commonParameters.getConnectionTimeout();
    httpClientBuilder.connectionTimeout(Duration.ofMillis(timeoutMs));
    httpClientBuilder.readTimeout(Duration.ofMillis(timeoutMs));
    return httpClientBuilder.build();
  }
}
