package org.mule.extension.mulechain.internal;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class AwsbedrockParams {

	@Parameter
	@Expression(ExpressionSupport.SUPPORTED)
	@OfValues(AwsbedrockRegionNameProvider.class)
	@Optional(defaultValue = "us-east-1")
	private String region;

	public String getRegion() {
		return region;
	}

}