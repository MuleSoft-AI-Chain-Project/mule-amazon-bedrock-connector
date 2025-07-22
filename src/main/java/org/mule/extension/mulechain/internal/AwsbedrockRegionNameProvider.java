package org.mule.extension.mulechain.internal;

import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsbedrockRegionNameProvider implements ValueProvider {

	private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
			"us-east-1",
			"us-east-2",
			"us-west-1",
			"us-west-2",
			"af-south-1",
			"ap-east-1",
			"ap-south-1",
			"ap-south-2",
			"ap-southeast-1",
			"ap-southeast-2",
			"ap-southeast-3",
			"ap-southeast-4",
			"ap-northeast-1",
			"ap-northeast-2",
			"ap-northeast-3",
			"ca-central-1",
			"eu-central-1",
			"eu-central-2",
			"eu-west-1",
			"eu-west-2",
			"eu-west-3",
			"eu-north-1",
			"eu-south-1",
			"eu-south-2",
			"me-south-1",
			"me-central-1",
			"sa-east-1",
			"us-gov-east-1",
			"us-gov-west-1");

	@Override
	public Set<Value> resolve() throws ValueResolvingException {
		return VALUES_FOR;
	}

}