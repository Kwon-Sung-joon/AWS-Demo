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
		
		private String username;
		private String accessKeyId;
		private String accessKeySecret;
		private String policy_arn;
		
		
		
		public String getPolicy_arn() {
			return policy_arn;
		}
		public void setPolicy_arn(String policy_arn) {
			this.policy_arn = policy_arn;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}

		public String getAccessKeyId() {
			return accessKeyId;
		}
		public void setAccessKeyId(String accessKeyId) {
			this.accessKeyId = accessKeyId;
		}
		public String getAccessKeySecret() {
			return accessKeySecret;
		}
		public void setAccessKeySecret(String accessKeySecret) {
			this.accessKeySecret = accessKeySecret;
		}
		
	}
}
