package com.rawchen.feishuchatdoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author RawChen
 * @date 2023-10-23 23:44
 */
@Component
@Data
@ConfigurationProperties(prefix = "custom")
public class Constants {

	public static final String CHAT_APP_ID = "f20ffxxx";

	public static final String CHAT_APP_SECRET = "NmY3ODE2ZmE4YjAxZjUzYzkwNTQ3Mxxx";

}
