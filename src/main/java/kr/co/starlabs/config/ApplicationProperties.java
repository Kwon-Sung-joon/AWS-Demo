package kr.co.starlabs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

	private final Aws aws = new Aws();

	public Aws getAws() {
		return aws;
	}

	public static class Aws {
		
		private String clientId;
		private String redirectUri;
		private String yourDomain;
		
		public String getClientId() {
			return clientId;
		}
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}
		public String getRedirectUri() {
			return redirectUri;
		}
		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}

		public String getYourDomain() {
			return yourDomain;
		}
		public void setYourDomain(String yourDomain) {
			this.yourDomain = yourDomain;
		}
	}
}
