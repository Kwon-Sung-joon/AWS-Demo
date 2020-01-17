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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;

import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;

import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.DeleteConflictException;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.DetachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DetachUserPolicyResult;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;

import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
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

		// EC2 정책에 생성된 유저 추가

		String policy_arn = applicationProperties.getAws().getPolicy_arn();

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
				.withPolicyArn(policy_arn);

		iam.attachUserPolicy(attach_request);

		System.out.println("Successfully attached policy " + policy_arn + " to user " + username);

		// 생성한 유저로 자격증명 생성
//		BasicAWSCredentials awsCreds = new BasicAWSCredentials(responseAccessKey.getAccessKey().getAccessKeyId(),
//				responseAccessKey.getAccessKey().getSecretAccessKey());
		resultMap.put("username", responseCreateUser.getUser().getUserName());

		// 생성한 유저의 액세스 키 ID,시크릿 키를 기본 값으로 지정
		applicationProperties.getAws().setUsername(username);
		applicationProperties.getAws().setAccessKeyId(responseAccessKey.getAccessKey().getAccessKeyId());
		applicationProperties.getAws().setAccessKeySecret(responseAccessKey.getAccessKey().getSecretAccessKey());

		return resultMap;
	}

	/**
	 * 
	 * @param name   = 태그명
	 * @param ami_id
	 * @return
	 */
	public Map<String, Object> createEC2(String name, String ami_id) {
		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("ap-northeast-2")
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		// final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		RunInstancesRequest run_request = new RunInstancesRequest().withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro).withMaxCount(1).withMinCount(1);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		Tag tag = new Tag().withKey("Name").withValue(name);

		CreateTagsRequest tag_request = new CreateTagsRequest().withTags(tag);

		CreateTagsResult tag_response = ec2.createTags(tag_request);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("reservation_id", reservation_id);
		resultMap.put("ami_id", ami_id);
		System.out.printf("Successfully started EC2 instance %s based on AMI %s", reservation_id, ami_id);

		return resultMap;
	}

	/**
	 * 
	 * @param instance_id
	 * @return
	 */
	public Map<String, Object> stopEC2(String instance_id) {
		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("ap-northeast-2")
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
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

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("instance_id", instance_id);

		return resultMap;
	}

	public Map<String, Object> descEC2(String instance_id) {

		Map<String, Object> resultMap = new HashMap<>();

		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
		AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("ap-northeast-2")
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		while (!done) {

			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					if (instance_id == instance.getInstanceId()) {
						resultMap.put("instance_id", instance.getInstanceId());
						resultMap.put("ami", instance.getImageId());
						resultMap.put("state", instance.getState().getName());
						resultMap.put("monitoring_state", instance.getMonitoring().getState());
						resultMap.put("launchTime", instance.getLaunchTime());
						System.out.println("같은 인스턴스 있음");
					}

				}
			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				done = true;
			}
		}

		return resultMap;

	}

	/**
	 * 
	 * @param instance_id
	 * @return
	 */
	public Map<String, Object> startEC2(String instance_id) {
//자격 증명 사용하기 ( 만들었던 자격증명을 이용하여 ec2를 생성하고 제어하는거 마무리 
		Map<String, Object> resultMap = new HashMap<>();

		System.out.println("액세스키 START EC2 " + applicationProperties.getAws().getAccessKeyId());
		System.out.println("액세스 비밀키 START EC2" + applicationProperties.getAws().getAccessKeySecret());
		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("ap-northeast-2")
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

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

		resultMap.put("instance_id", instance_id);

		return resultMap;
	}

	public Map<String, Object> logout() {
		String username = applicationProperties.getAws().getUsername();
		String policy_arn = applicationProperties.getAws().getPolicy_arn();

		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		DetachUserPolicyRequest requestDetachUserPolicy = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn(policy_arn);

		DetachUserPolicyResult responseDetachUserPolicy = iam.detachUserPolicy(requestDetachUserPolicy);

		System.out.println("Successfully detached policy " + policy_arn + " from role " + username);

		DeleteUserRequest requestDeleteUser = new DeleteUserRequest().withUserName(username);

		try {
			iam.deleteUser(requestDeleteUser);
		} catch (DeleteConflictException e) {
			System.out.println("Unable to delete user. Verify user is not" + " associated with any resources");
			throw e;
		}

		return null;
	}

	public ArrayList<Object> listEC2() {
		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion("ap-northeast-2")
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
		boolean done = false;

		DescribeInstancesRequest request = new DescribeInstancesRequest();
		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					resultList.add(i, instance.getInstanceId());
					i++;
				}
			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				done = true;
			}
		}

		for (Object j : resultList) {
			System.out.println("값 : " + j);
		}
		return resultList;

	}

}
