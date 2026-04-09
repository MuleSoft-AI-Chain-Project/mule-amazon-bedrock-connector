package com.mulesoft.connectors.bedrock.internal.connection.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import com.mulesoft.connectors.bedrock.api.parameter.ProxyParameterGroup;
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

  private static final String[] FIPS_PROVIDER_NAMES = {"BCJSSE", "BCFIPS", "SunPKCS11-NSS-FIPS"};
  private static final String[] FIPS_TLS_ALGORITHMS = {"PKIX", "SunX509", "X509"};

  public SdkHttpClientFactory(ProxyParameterGroup proxyParameterGroup) {
    this.proxyParameterGroup = proxyParameterGroup;
  }

  private Provider findFipsProvider() {
    for (Provider provider : Security.getProviders()) {
      String providerName = provider.getName();
      for (String fipsName : FIPS_PROVIDER_NAMES) {
        if (providerName.contains(fipsName)) {
          LOGGER.debug("Found FIPS provider: {}", providerName);
          return provider;
        }
      }
    }
    return null;
  }

  private TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException {
    Provider fipsProvider = findFipsProvider();
    if (fipsProvider != null) {
      for (String algorithm : FIPS_TLS_ALGORITHMS) {
        try {
          TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm, fipsProvider);
          LOGGER.debug("Using TrustManagerFactory with algorithm {} from provider {}", algorithm, fipsProvider.getName());
          return tmf;
        } catch (NoSuchAlgorithmException ignored) {
          LOGGER.trace("Algorithm {} not available from provider {}", algorithm, fipsProvider.getName());
        }
      }
    }
    String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    LOGGER.debug("Using default TrustManagerFactory algorithm: {}", defaultAlgorithm);
    return TrustManagerFactory.getInstance(defaultAlgorithm);
  }

  private SSLContext createSslContext(CommonParameters commonParameters)
      throws NoSuchAlgorithmException, KeyManagementException {
    if (commonParameters.getTlsContext() != null) {
      try {
        return commonParameters.getTlsContext().createSslContext();
      } catch (NoSuchAlgorithmException e) {
        LOGGER.info("Default SSLContext creation failed, attempting FIPS-compatible creation: {}", e.getMessage());
        return createFipsCompatibleSslContext();
      }
    }
    return createFipsCompatibleSslContext();
  }

  private SSLContext createFipsCompatibleSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    Provider fipsProvider = findFipsProvider();
    String[] protocols = {"TLSv1.3", "TLSv1.2", "TLS"};

    for (String protocol : protocols) {
      if (fipsProvider != null) {
        try {
          SSLContext sslContext = SSLContext.getInstance(protocol, fipsProvider);
          sslContext.init(null, null, null);
          LOGGER.debug("Created SSLContext with protocol {} from FIPS provider {}", protocol, fipsProvider.getName());
          return sslContext;
        } catch (NoSuchAlgorithmException ignored) {
          LOGGER.trace("Protocol {} not available from FIPS provider {}", protocol, fipsProvider.getName());
        }
      }
      try {
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null, null, null);
        LOGGER.info("Created SSLContext with protocol {}", protocol);
        return sslContext;
      } catch (NoSuchAlgorithmException ignored) {
        LOGGER.info("Protocol {} not available from default providers", protocol);
      }
    }
    throw new NoSuchAlgorithmException("No suitable TLS protocol found in available security providers");
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
        SSLContext sslContext = createSslContext(commonParameters);
        clientBuilder.socketFactory(new SSLConnectionSocketFactory(sslContext));
      } catch (KeyManagementException | NoSuchAlgorithmException e) {
        LOGGER.info("SSL/TLS configuration error: {}", e.getMessage());
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
        KeyStore trustStore = loadKeyStore(keyType, keyPath, keyPassword);
        TrustManagerFactory trustManagerFactory = createTrustManagerFactory();
        trustManagerFactory.init(trustStore);
        clientBuilder.tlsTrustManagersProvider(trustManagerFactory::getTrustManagers);
      } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
        LOGGER.info("TLS configuration error: {}", e.getMessage());
        throw new ConnectionException("TLS configuration error", e);
      }
    }
  }

  private KeyStore loadKeyStore(String keyType, String keyPath, String keyPassword)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    Provider fipsProvider = findFipsProvider();
    KeyStore keyStore = null;

    if (fipsProvider != null) {
      try {
        keyStore = KeyStore.getInstance(keyType, fipsProvider);
        LOGGER.debug("Using KeyStore type {} from FIPS provider {}", keyType, fipsProvider.getName());
      } catch (KeyStoreException e) {
        LOGGER.debug("KeyStore type {} not available from FIPS provider, falling back to default", keyType);
      }
    }

    if (keyStore == null) {
      keyStore = KeyStore.getInstance(keyType);
      LOGGER.debug("Using KeyStore type {} from default provider", keyType);
    }

    try (FileInputStream fis = new FileInputStream(keyPath)) {
      keyStore.load(fis, keyPassword != null ? keyPassword.toCharArray() : null);
    }
    return keyStore;
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
    long timeoutMs = readTimeoutMsOverride != null ? readTimeoutMsOverride : (long) commonParameters.getConnectionTimeout();
    httpClientBuilder.connectionTimeout(Duration.ofMillis(timeoutMs));
    httpClientBuilder.readTimeout(Duration.ofMillis(timeoutMs));
    return httpClientBuilder.build();
  }
}
