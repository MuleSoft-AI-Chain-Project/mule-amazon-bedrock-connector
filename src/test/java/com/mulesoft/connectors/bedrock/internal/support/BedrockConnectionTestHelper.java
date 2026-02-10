package com.mulesoft.connectors.bedrock.internal.support;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import com.mulesoft.connectors.bedrock.internal.connection.provider.BasicConnectionProvider;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import org.mule.runtime.api.connection.ConnectionException;

/**
 * Builds a BedrockConnection for integration tests using credentials from automation-credentials.properties.
 */
public final class BedrockConnectionTestHelper {

  private BedrockConnectionTestHelper() {}

  /**
   * Creates a BedrockConnection using the given credentials. Caller must ensure credentials are available.
   *
   * @param creds credentials from AutomationCredentials.load()
   * @return connected BedrockConnection
   * @throws ConnectionException if connection fails
   */
  public static BedrockConnection createConnection(AutomationCredentials creds) throws ConnectionException {
    CommonParameters commonParams = buildCommonParameters(creds);
    BasicConnectionProvider provider = new BasicConnectionProvider();
    provider.setCommonParameters(commonParams);
    setSessionToken(provider, creds.getSessionToken());
    return provider.connect();
  }

  private static CommonParameters buildCommonParameters(AutomationCredentials creds) {
    CommonParameters p = new CommonParameters();
    p.setAccessKey(creds.getAccessKey());
    p.setSecretKey(creds.getSecretKey());
    p.setRegion(creds.getRegion() != null ? creds.getRegion().trim() : "us-east-1");
    p.setTryDefaultAWSCredentialsProviderChain(false);
    p.setConnectionTimeout(50);
    p.setSocketTimeout(50);
    p.setMaxConnections(50);
    setField(p, "connectionTimeoutUnit", TimeUnit.SECONDS);
    setField(p, "socketTimeoutUnit", TimeUnit.SECONDS);
    return p;
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Could not set " + fieldName, e);
    }
  }

  private static void setSessionToken(BasicConnectionProvider provider, String sessionToken) {
    try {
      Field f = BasicConnectionProvider.class.getDeclaredField("sessionToken");
      f.setAccessible(true);
      f.set(provider, sessionToken);
    } catch (Exception e) {
      throw new RuntimeException("Could not set sessionToken on BasicConnectionProvider", e);
    }
  }
}
