package kr.co.starlabs.service.aws;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;

import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;

import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;

import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

import kr.co.starlabs.config.ApplicationProperties;

@Service
public class AwsService {

	private static final Logger logger = LoggerFactory.getLogger(AwsService.class);

	@Autowired
	private ApplicationProperties applicationProperties;

// Cognito 예시
//	public Map<String, Object> getAwsCodeUrl() {
//
//		Map<String, Object> resultMap = new HashMap<>();
//
//		String yourDomain = applicationProperties.getAws().getYourDomain();
//		String redirectUri = applicationProperties.getAws().getRedirectUri();
//		String clientId = applicationProperties.getAws().getClientId();
//
//		resultMap.put("yourDomain", yourDomain);
//		resultMap.put("redirectUri", redirectUri);
//		resultMap.put("clientId", clientId);
//
//		logger.debug("login parameters [{}, {}, {}, {}]", yourDomain, redirectUri, clientId);
//
//		return resultMap;
//	}
	
	/**
	 * 
	 * @param username
	 * @return
	 */
	
	public Map<String, Object> createUser(String username) {

		// 유저를 생성하고 액세스 키 생성,
		// 유저의 ARN을 사용하여 정책을 준다.
		Map<String, Object> resultMap = new HashMap<>();

		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		// 유저 생성
		CreateUserRequest requestCreateUser = new CreateUserRequest().withUserName(username);
		CreateUserResult responseCreateUser = iam.createUser(requestCreateUser);

		// 생성한 유저에 액세스 키 생성
		CreateAccessKeyRequest requestAccessKey = new CreateAccessKeyRequest().withUserName(username);
		CreateAccessKeyResult responseAccessKey = iam.createAccessKey(requestAccessKey);

		resultMap.put("username", responseCreateUser.getUser().getUserName());

		resultMap.put("accesskey", responseAccessKey.getAccessKey());

		// EC2 정책에 생성된 유저 추가
		final String POLICY_ARN = "arn:aws:iam::aws:policy/AmazonEC2FullAccess";

		ListAttachedUserPoliciesRequest requestAttachedUserPolicies = new ListAttachedUserPoliciesRequest()
				.withUserName(username);
		List<AttachedPolicy> matching_policies = new ArrayList<>();

		boolean done = false;

		while (!done) {
			ListAttachedUserPoliciesResult responseAttachedUserPolicies = iam
					.listAttachedUserPolicies(requestAttachedUserPolicies);

			matching_policies.addAll(responseAttachedUserPolicies.getAttachedPolicies().stream()
					.filter(p -> p.getPolicyName().equals(username)).collect(Collectors.toList()));

			if (!responseAttachedUserPolicies.getIsTruncated()) {
				done = true;
			}
			requestAttachedUserPolicies.setMarker(responseAttachedUserPolicies.getMarker());
		}

		if (matching_policies.size() > 0) {
			System.out.println(username + " policy is already attached to this role.");
			System.exit(1);
		}

		AttachUserPolicyRequest attach_request = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn(POLICY_ARN);

		iam.attachUserPolicy(attach_request);

		System.out.println("Successfully attached policy " + POLICY_ARN + " to user " + username);

		return resultMap;
	}

	public Map<String, Object> startEC2(String instance_id) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		DryRunSupportedRequest<StartInstancesRequest> dry_request = () -> {
			StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) {
			System.out.printf("Failed dry run to start instance %s", instance_id);

			throw dry_response.getDryRunResponse();
		}

		StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instance_id);

		ec2.startInstances(request);

		System.out.printf("Successfully started instance %s", instance_id);
		return null;
	}

	
	
	public Map<String, Object> stopEC2(String instance_id) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		DryRunSupportedRequest<StopInstancesRequest> dry_request = () -> {
			StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) {
			System.out.printf("Failed dry run to stop instance %s", instance_id);
			throw dry_response.getDryRunResponse();
		}

		StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instance_id);

		ec2.stopInstances(request);

		System.out.printf("Successfully stop instance %s", instance_id);
		return null;
	}

}
