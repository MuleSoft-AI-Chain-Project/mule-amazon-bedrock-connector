package org.mule.extension.aws.connection.provider.credentials;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.mule.extension.bedrock.internal.error.BedrockErrorType;
import org.mule.extension.bedrock.internal.error.exception.BedrockException;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.RegionDisabledException;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

public class AssumeRoleCredentialsProvider implements AwsCredentialsProvider {

  private final StsClient stsClient;
  private final String role;
  private final String externalID;
  private final AtomicReference<AwsCredentials> currentCredentials = new AtomicReference<>();

  private Instant expiration;

  public AssumeRoleCredentialsProvider(String role, StsClient stsClient, String externalID) {
    this.stsClient = stsClient;
    this.role = Validate.notNull(role, "ConsumerRole must not be null.");
    this.externalID = externalID;
    this.refreshCredentials();
  }

  public void refreshCredentials() {
    AssumeRoleResponse assumeRoleResponse = assumeRole(stsClient, role);

    this.currentCredentials.set(AwsSessionCredentials.create(assumeRoleResponse.credentials().accessKeyId(),
                                                             assumeRoleResponse.credentials().secretAccessKey(),
                                                             assumeRoleResponse.credentials().sessionToken()));
    expiration = assumeRoleResponse.credentials().expiration();
  }

  @Override
  public AwsCredentials resolveCredentials() {
    if (Objects.isNull(currentCredentials.get()) || Instant.now().isAfter(expiration.minus(Duration.ofMinutes(5)))) {
      refreshCredentials();
    }
    return currentCredentials.get();
  }

  @Override
  public String toString() {
    return ToString.builder("StaticCredentialsProvider")
        .add("credentials", currentCredentials.get())
        .build();
  }

  /**
   * Assumes role for given credentials and returns a set of temporary security credentials.
   *
   * @param stsClient STS service client used to assume a role
   * @return a new sts credentials.
   */
  private AssumeRoleResponse assumeRole(StsClient stsClient, String role) {
    AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
        .roleArn(role)
        .roleSessionName("bedrock-role-" + UUID.randomUUID())
        .externalId(externalID);

    try {
      return stsClient.assumeRole(assumeRoleRequestBuilder.build());
    } catch (RegionDisabledException e) {
      throw new BedrockException("STS is not activated in the requested region for the account that is being asked to generate credentials. The account administrator must use the IAM console to activate STS in that region.",
                                 BedrockErrorType.AUTHORIZATION_NOT_FOUND);
    }
  }
}
