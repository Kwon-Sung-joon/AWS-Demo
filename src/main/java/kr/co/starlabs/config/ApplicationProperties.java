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
		private String ami_id;
		private String region;

		public String getRegion() {
			return region;
		}
		public void setRegion(String region) {
			this.region = region;
		}
		public String getAmi_id() {
			return ami_id;
		}
		public void setAmi_id(String ami_id) {
			this.ami_id = ami_id;
		}
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
