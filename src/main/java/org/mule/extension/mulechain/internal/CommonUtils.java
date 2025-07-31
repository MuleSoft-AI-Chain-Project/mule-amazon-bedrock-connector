package org.mule.extension.mulechain.internal;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

public class CommonUtils {
	
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);


    // Make it public so other classes (like helper or connection provider) can use it
    public static ChronoUnit convertToChronoUnit(TimeUnitEnum unit) {
        switch (unit) {
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            default:
                return ChronoUnit.MILLIS;
        }
    }

    // Optional utility to directly create a Duration
    public static Duration toDuration(int timeout, TimeUnitEnum unit) {
        return Duration.of(timeout, convertToChronoUnit(unit));
    }
    

    public static SdkHttpClient buildHttpClientWithTimeout(Integer timeout, TimeUnitEnum unit) {

        int effectiveTimeout = (timeout != null) ? timeout : 10;
        TimeUnitEnum effectiveUnit = (unit != null) ? unit : TimeUnitEnum.SECONDS;
        Duration timeoutDuration = toDuration(effectiveTimeout, effectiveUnit);

        logger.debug("HTTP client timeout set to: {} {}", effectiveTimeout, effectiveUnit);

        return UrlConnectionHttpClient.builder()
                .socketTimeout(timeoutDuration)
                .build();
    }
}
