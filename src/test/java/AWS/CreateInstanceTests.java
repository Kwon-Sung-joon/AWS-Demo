package AWS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;


import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;

/**
 * Creates an EC2 instance
 */
public class CreateInstanceTests {
	public static void main(String[] args) {

		String name = "d1jd9281";
		String ami_id = "ami-0bea7fd38fabe821a";
		String username = UUID.randomUUID().toString();
		Map<String, Object> resultMap = new HashMap<>();

		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		// 유저 생성
		CreateUserRequest requestCreateUser = new CreateUserRequest().withUserName(username);
		CreateUserResult responseCreateUser = iam.createUser(requestCreateUser);

		// 생성한 유저에 액세스 키 생성
		CreateAccessKeyRequest requestAccessKey = new CreateAccessKeyRequest().withUserName(username);
		CreateAccessKeyResult responseAccessKey = iam.createAccessKey(requestAccessKey);
		
		resultMap.put("username", responseCreateUser.getUser().getUserName());
		
		resultMap.put("accesskeyId", responseAccessKey.getAccessKey().getAccessKeyId());
		resultMap.put("accesskeySec", responseAccessKey.getAccessKey().getSecretAccessKey());
		
		System.out.println(responseAccessKey.getAccessKey().getAccessKeyId());
		System.out.println(responseAccessKey.getAccessKey().getSecretAccessKey());

		
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

		AttachUserPolicyRequest attach_request = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn(POLICY_ARN);

		iam.attachUserPolicy(attach_request);

		BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAZZL2R2GQNVDCQT6X", "bjVRWsFl+YjRcA3CHwTiOACs7m+6Ring/p3Z37Xy");
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder
				  .standard()
				  .withRegion("ap-northeast-2")
				  .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				  .build();
		//final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		
		
		RunInstancesRequest run_request = new RunInstancesRequest().withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro).withMaxCount(1).withMinCount(1);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		Tag tag = new Tag().withKey("Name").withValue(name);

		CreateTagsRequest tag_request = new CreateTagsRequest().withTags(tag);

		CreateTagsResult tag_response = ec2.createTags(tag_request);

		System.out.printf("Successfully started EC2 instance %s based on AMI %s", reservation_id, ami_id);
	}
}