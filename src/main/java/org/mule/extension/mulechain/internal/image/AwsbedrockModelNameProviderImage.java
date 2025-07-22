package org.mule.extension.mulechain.internal.image;

import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsbedrockModelNameProviderImage implements ValueProvider {

	private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
			"amazon.titan-image-generator-v2:0",
			"amazon.titan-image-generator-v1",
			"stability.stable-diffusion-xl-v1",
			"amazon.nova-canvas-v1:0");

	@Override
	public Set<Value> resolve() throws ValueResolvingException {

		return VALUES_FOR;
	}

}