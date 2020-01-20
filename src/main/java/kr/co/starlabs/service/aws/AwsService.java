package kr.co.starlabs.service.aws;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyResult;
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
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;

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
	 * 새로운 IAM 유저 생성
	 * 
	 * @param username
	 * @return
	 */

	public Map<String, Object> createUser(String username) {

		Map<String, Object> resultMap = new HashMap<>();

		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		// IAM 유저 생성
		CreateUserRequest requestCreateUser = new CreateUserRequest().withUserName(username);
		CreateUserResult responseCreateUser = iam.createUser(requestCreateUser);

		// 생성한 IAM 유저에 액세스 키 생성
		CreateAccessKeyRequest requestAccessKey = new CreateAccessKeyRequest().withUserName(username);
		CreateAccessKeyResult responseAccessKey = iam.createAccessKey(requestAccessKey);

		// 생성한 IAM 유저에 정책 부여
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

		resultMap.put("username", responseCreateUser.getUser().getUserName());

		// 생성한 유저의 액세스 키 ID,시크릿 키를 기본 값으로 지정
		applicationProperties.getAws().setUsername(username);
		applicationProperties.getAws().setAccessKeyId(responseAccessKey.getAccessKey().getAccessKeyId());
		applicationProperties.getAws().setAccessKeySecret(responseAccessKey.getAccessKey().getSecretAccessKey());

		return resultMap;
	}

	/**
	 * 
	 * 생성한 IAM 유저 삭제
	 */
	public Map<String, Object> logout() {
		String username = applicationProperties.getAws().getUsername();
		String policy_arn = applicationProperties.getAws().getPolicy_arn();
		String access_key = applicationProperties.getAws().getAccessKeyId();

		/*
		 * 유저 삭제 순서 정책 제거 -> 액세스 키 제거 -> 유저 삭제
		 */
		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		DetachUserPolicyRequest requestDetachUserPolicy = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn(policy_arn);

		DetachUserPolicyResult responseDetachUserPolicy = iam.detachUserPolicy(requestDetachUserPolicy);

		System.out.println("Successfully detached policy " + policy_arn + " from role " + username);

		DeleteAccessKeyRequest request = new DeleteAccessKeyRequest().withAccessKeyId(access_key)
				.withUserName(username);

		DeleteAccessKeyResult response = iam.deleteAccessKey(request);

		System.out.println("Successfully deleted access key " + access_key + " from user " + username);
		DeleteUserRequest requestDeleteUser = new DeleteUserRequest().withUserName(username);

		try {
			iam.deleteUser(requestDeleteUser);
		} catch (DeleteConflictException e) {
			System.out.println("Unable to delete user. Verify user is not" + " associated with any resources");
			throw e;
		}

		return null;
	}

	/**
	 * ec2 클라이언트 생성자
	 * 
	 * @return
	 */
	public AmazonEC2 ec2Client() {

		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();
		String region = applicationProperties.getAws().getRegion();

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
		AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
		return ec2;
	}
	
	/**
	 * 현재 계정에 있는 EC2 인스턴스 목록 출력
	 * 
	 * @return
	 */

	public ArrayList<Object> listEC2() {
		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		AmazonEC2 ec2 = ec2Client();

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

		return resultList;

	}

	/**
	 * 
	 * 새로운 인스턴스 생성
	 * 
	 * @return
	 */
	public Map<String, Object> createEC2(String name) {

		String ami_id = applicationProperties.getAws().getAmi_id();

		AmazonEC2 ec2 = ec2Client();

		RunInstancesRequest run_request = new RunInstancesRequest().withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro).withMaxCount(1).withMinCount(1);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		Tag tag = new Tag().withKey("Name").withValue(name);

		CreateTagsRequest tag_request = new CreateTagsRequest().withTags(tag);

		// CreateTagsResult tag_response = ec2.createTags(tag_request);
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("instance_id", reservation_id);

		return resultMap;
	}

	/**
	 * 
	 * 인스턴스 시작
	 * 
	 * @return
	 */
	public Map<String, Object> startEC2(String instance_id) {
		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

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

	/**
	 * 
	 * 인스턴스 중지
	 */
	public Map<String, Object> stopEC2(String instance_id) {

		AmazonEC2 ec2 = ec2Client();
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

	/**
	 * 
	 * 인스턴스 종료
	 * 
	 * @return
	 */
	public Map<String, Object> terminateEC2(String instance_id) {
		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

		DryRunSupportedRequest<TerminateInstancesRequest> dry_request = () -> {
			TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) {
			System.out.printf("Failed dry run to start instance %s", instance_id);

			throw dry_response.getDryRunResponse();
		}

		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instance_id);

		ec2.terminateInstances(request);

		System.out.printf("Successfully terminated instance %s", instance_id);

		resultMap.put("instance_id", instance_id);

		return resultMap;
	}

	/**
	 * 
	 * 인스턴스의 정보 출력
	 * 
	 * @return
	 */
	public Map<String, Object> descEC2(String instance_id) {

		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		while (!done) {

			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					if (instance_id.equals(instance.getInstanceId())) {
						resultMap.put("instance_id", instance.getInstanceId());
						resultMap.put("ami", instance.getImageId());
						resultMap.put("state", instance.getState().getName());
						resultMap.put("type",instance.getInstanceType());
						resultMap.put("monitoring_state", instance.getMonitoring().getState());
						resultMap.put("launchTime", instance.getLaunchTime());
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

}
