package org.mule.extension.bedrock.internal.connection.provider;

import org.mule.extension.aws.connection.provider.AssumeRoleConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("role")
public class ParentAssumeRoleConnectionProvider extends AssumeRoleConnectionProvider {

  public ParentAssumeRoleConnectionProvider() {
    super();
  }
}
