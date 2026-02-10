package com.mulesoft.connectors.bedrock.internal.support;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads AWS credentials from automation-credentials.properties for integration tests. Uses config.accessKey, config.secretKey,
 * config.region, config.security.token (keys may have trailing space).
 */
public final class AutomationCredentials {

  private static final Logger LOG = LoggerFactory.getLogger(AutomationCredentials.class);
  private static final String RESOURCE = "automation-credentials.properties";

  private final String accessKey;
  private final String secretKey;
  private final String sessionToken;
  private final String region;

  private AutomationCredentials(String accessKey, String secretKey, String sessionToken, String region) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.sessionToken = sessionToken;
    this.region = region;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public String getSessionToken() {
    return sessionToken;
  }

  public String getRegion() {
    return region;
  }

  public boolean isAvailable() {
    return accessKey != null && !accessKey.isBlank()
        && secretKey != null && !secretKey.isBlank()
        && region != null && !region.isBlank();
  }

  /**
   * Loads credentials from classpath automation-credentials.properties. Returns an instance with nulls if file is missing or keys
   * not set.
   */
  public static AutomationCredentials load() {
    try (InputStream in = AutomationCredentials.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (in == null) {
        LOG.debug("{} not found on classpath", RESOURCE);
        return new AutomationCredentials(null, null, null, null);
      }
      Properties props = new Properties();
      props.load(in);
      String accessKey = getProp(props, "config.accessKey");
      String secretKey = getProp(props, "config.secretKey");
      String region = getProp(props, "config.region");
      String sessionToken = getProp(props, "config.security.token");
      return new AutomationCredentials(accessKey, secretKey, sessionToken, region);
    } catch (Exception e) {
      LOG.warn("Failed to load {}: {}", RESOURCE, e.getMessage());
      return new AutomationCredentials(null, null, null, null);
    }
  }

  private static String getProp(Properties props, String key) {
    String v = props.getProperty(key);
    if (v != null) {
      return v.trim();
    }
    v = props.getProperty(key + " ");
    return v != null ? v.trim() : null;
  }
}
