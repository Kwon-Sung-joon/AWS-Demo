package kr.co.starlabs.service.aws;

import java.util.HashMap;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.costexplorer.model.DateInterval;
import com.amazonaws.services.costexplorer.model.DimensionValues;
import com.amazonaws.services.costexplorer.model.Expression;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageRequest;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageResult;
import com.amazonaws.services.costexplorer.model.GetCostForecastRequest;
import com.amazonaws.services.costexplorer.model.GetDimensionValuesRequest;
import com.amazonaws.services.costexplorer.model.GetDimensionValuesResult;
import com.amazonaws.services.costexplorer.model.Granularity;
import com.amazonaws.services.costexplorer.model.Group;
import com.amazonaws.services.costexplorer.model.GroupDefinition;
import com.amazonaws.services.costexplorer.model.ResultByTime;
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
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;

import com.amazonaws.services.logs.model.ResourceAlreadyExistsException;

import java.util.Date;
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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest;

import kr.co.starlabs.config.ApplicationProperties;

/**
 * 
 * @author sjkwon
 *
 */
@Service
public class AwsService 
{

	private static final Logger logger = LoggerFactory.getLogger(AwsService.class);

	@Autowired
	private ApplicationProperties applicationProperties;

	/**
	 * 새로운 IAM 유저 생성
	 * 
	 * @param username
	 * @return
	 */
	public Map<String, Object> createUser(String username) 
	{

		Map<String, Object> resultMap = new HashMap<>();
		
		//처음 IAM 사용자를 생성하기 위해서는 로컬에 저장되어있는 자격증명 파일을 사용하여 생성한다. 해당 자격증명 파일에는 IAM Full Access 정책이 추가되어있는 계정이여야만 한다. 
		AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();
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

		while (!done) 
		{
			ListAttachedUserPoliciesResult responseAttachedUserPolicies = iam
					.listAttachedUserPolicies(requestAttachedUserPolicies);

			matching_policies.addAll(responseAttachedUserPolicies.getAttachedPolicies().stream()
					.filter(p -> p.getPolicyName().equals(username)).collect(Collectors.toList()));

			if (!responseAttachedUserPolicies.getIsTruncated()) 
			{
				done = true;
			}
			requestAttachedUserPolicies.setMarker(responseAttachedUserPolicies.getMarker());
		}

		if (matching_policies.size() > 0) 
		{
			System.out.println(username + " policy is already attached to this role.");
			System.exit(1);
		}

		// Cloud Watch Logs, Events 정책 추가

		AttachUserPolicyRequest attach_request = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn(policy_arn);

		iam.attachUserPolicy(attach_request);

		// CloudWatch, Events, Cost Explorer 정책을 추가하기 위해여 해당 정책의 arn을 가져와 정책을 연결한다. 
		AttachUserPolicyRequest attach_request2 = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchLogsFullAccess");
		
		iam.attachUserPolicy(attach_request2);
		
		AttachUserPolicyRequest attach_request3 = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchEventsFullAccess");

		iam.attachUserPolicy(attach_request3);
		
		// Cost Explorer는 AWS 관리 정책이 아니고 사용자 관리 정책이기 때문에 해당 계정에서 Cost Explorer 정책을 추가 하여야만 사용할 수 있다. 
		// policy 앞 부분의 숫자는 계정을 나타내는데, 이는 현재 로컬 자격증명 파일에 저장되어있는 계정이여야만 한다. 

		AttachUserPolicyRequest attach_request4 = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::879873534644:policy/AWSCostExplorerServiceFullAccess");

		iam.attachUserPolicy(attach_request4);


		// 생성한 유저의 액세스 키 ID,시크릿 키를 기본 값으로 지정
		applicationProperties.getAws().setUsername(username);
		applicationProperties.getAws().setAccessKeyId(responseAccessKey.getAccessKey().getAccessKeyId());
		applicationProperties.getAws().setAccessKeySecret(responseAccessKey.getAccessKey().getSecretAccessKey());

		resultMap.put("username", responseCreateUser.getUser().getUserName());
		logger.debug("username [{}]", username);

		return resultMap;
	}

	/**
	 * 
	 * 생성한 IAM 유저 삭제
	 */
	public void logout() 
	{
		String username = applicationProperties.getAws().getUsername();
		String policy_arn = applicationProperties.getAws().getPolicy_arn();
		String access_key = applicationProperties.getAws().getAccessKeyId();


		/**
		 * 기존 유저가 아닌, 앞서 새로 생성한 유저를 삭제하기 위해서는 가장 먼저 유저에 연결되어있는 정책을 모두 제거한뒤, 
		 * 유저를 생성하며 함께 생성한 액세스 키를 삭제해야만 유저를 삭제할 수 있다. 
		 */
		
		//생성했던 사용자를 삭제할 것이기 때문에, 로컬의 자격증명 파일을 사용한다.
		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		//정책을 모두 제거함
		DetachUserPolicyRequest requestDetachUserPolicy = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn(policy_arn);

		iam.detachUserPolicy(requestDetachUserPolicy);

		DetachUserPolicyRequest requestDetachUserPolicy2 = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchLogsFullAccess");

		iam.detachUserPolicy(requestDetachUserPolicy2);

		DetachUserPolicyRequest requestDetachUserPolicy3 = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchEventsFullAccess");

		iam.detachUserPolicy(requestDetachUserPolicy3);

		DetachUserPolicyRequest requestDetachUserPolicy4 = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::879873534644:policy/AWSCostExplorerServiceFullAccess");

		iam.detachUserPolicy(requestDetachUserPolicy4);
		
			

		//액세스키 제거 
		DeleteAccessKeyRequest request = new DeleteAccessKeyRequest().withAccessKeyId(access_key)
				.withUserName(username);

		DeleteAccessKeyResult response = iam.deleteAccessKey(request);

		DeleteUserRequest requestDeleteUser = new DeleteUserRequest().withUserName(username);

		try 
		{
			iam.deleteUser(requestDeleteUser);
		} catch (DeleteConflictException e) 
		{
			System.out.println("Unable to delete user. Verify user is not" + " associated with any resources");
			throw e;
		}

	}

	/**EC2 클라이언트 생성 함수 
	 * EC2 서비스를 사용하기 위해서는 매번 클라이언트 빌더를 생성해야한다.
	 * @return
	 */
	public AmazonEC2 ec2Client() 
	{

		//현재 설정되어있는 액세스 ID, 시크릿키, 그리고 리전을 사용하기 위하여 받아온다.
		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();
		String region = applicationProperties.getAws().getRegion();

		//EC2 빌더를 사용하기 위해 사용할 자격증명 설정
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, accessKeySecret);
		//EC2 클라이언트 빌더 생성  
		AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region)
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
		return ec2;
	}

	/**
	 * 현재 계정에 있는 EC2 인스턴스 목록 출력
	 * @return
	 */

	public ArrayList<Object> listEC2() 
	{
		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		//EC2 클라이언트 함수를 호출하여 빌더를 받아온다.
		AmazonEC2 ec2 = ec2Client();

		DescribeInstancesRequest request = new DescribeInstancesRequest();
		boolean done = false;

		while (!done) 
		{
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) 
			{
				for (Instance instance : reservation.getInstances()) 
				{

					Map<String, Object> resultMap = new HashMap<>();
					resultMap.put("instance_id", instance.getInstanceId());
					resultMap.put("state", instance.getState().getName());

					resultList.add(i, resultMap);

					logger.debug("listEc2 parameters [{}, {}]", instance.getInstanceId(),
							instance.getState().getName());
					i++;
				}
			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) 
			{
				done = true;
			}
		}

		return resultList;

	}

	/**
	 * 인스턴스 시작
	 * 
	 * @param name
	 * @return
	 */
	public Map<String, Object> createEC2(String name) 
	{

		String ami_id = applicationProperties.getAws().getAmi_id();

		AmazonEC2 ec2 = ec2Client();
		String secureGroups = applicationProperties.getAws().getSecureGroups();
		String accessKeyName = applicationProperties.getAws().getAccessKeyName();
		// test 키페어는 인스턴스 연결에 사용할 키페어로 미리 생성해두어야 한다.
		// AlloSSH 보안그룹은 콘솔에서 미리 생성한 보안그룹으로, 22포트 인바운드를 열어두었다. (인스턴스 연결을 위해)
		RunInstancesRequest run_request = new RunInstancesRequest().withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro).withMaxCount(1).withMinCount(1).withKeyName(accessKeyName)
				.withSecurityGroups(secureGroups);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		Tag tag = new Tag().withKey("Name").withValue(name);

		CreateTagsRequest tag_request = new CreateTagsRequest().withTags(tag);
		// CreateTagsResult tag_response = ec2.createTags(tag_request);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("instance_id", reservation_id);
		logger.debug("instance_id [{}]", reservation_id);

		// CloudWatch 규칙생성 및 로그그룹 연결

		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, accessKeySecret);

		CloudWatchEventsClient cwe = CloudWatchEventsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).build();
		// 기본 루트 클라이언트로 생성하므로 수정 필요
		CloudWatchLogsClient cwl = CloudWatchLogsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(Region.AP_NORTHEAST_2).build();

		String rule_name = reservation_id;
		// 로그그룹 생성 => 인스턴스 id로 생성
		String logGroupName = "/aws/events/" + reservation_id;
		String logArn = "";

		// 매개변수 instance_id 를 eventPattern 에 넣기
		// state를 모두 확인할지, 받아서 확인할지 체크
		String eventPattern = "{\r\n" + "  \"source\": [\r\n" + "    \"aws.ec2\"\r\n" + "  ],\r\n"
				+ "  \"detail-type\": [\r\n" + "    \"EC2 Instance State-change Notification\"\r\n" + "  ],\r\n"
				+ "  \"detail\": {\r\n" + "    \"state\": [\r\n" + "      \"stopped\"\r\n" + "    ],\r\n"
				+ "    \"instance-id\": [\r\n" + "      \"" + reservation_id + "\"\r\n" + "    ]\r\n" + "  }\r\n" + "}";

		try 
		{
			PutRuleRequest ruleRequest = PutRuleRequest.builder().name(rule_name).state(RuleState.ENABLED)
					.eventPattern(eventPattern).build();
			PutRuleResponse ruleResponse = cwe.putRule(ruleRequest);
			System.out.println(
					"Successfully created CloudWatch events rule " + rule_name + " with arn " + ruleResponse.ruleArn());

		} catch (ResourceAlreadyExistsException expected) 
		{
			// 규칙이 이미 있다면 추가할 필요 없음
			System.out.println("Log Events Rule already exists");
			// Ignored or expected.
		}

		try 
		{

			CreateLogGroupRequest logRequest = CreateLogGroupRequest.builder().logGroupName(logGroupName).build();
			CreateLogGroupResponse logResponse = cwl.createLogGroup(logRequest);

			System.out.println("Successfully create CloudWatch log Groups " + logResponse);

		} catch (software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException expected) 
		{
			// 로그 그룹이 있을 시에는 생성할 필요 없으므로 예외처리
			System.out.println("Log Events already exists");
			// Ignored or expected.
		} finally 
		{

			// 로그 데이터를 가져올 때 arn이 필요하므로 arn 가져오기
			DescribeLogGroupsResponse descLogs = cwl.describeLogGroups();
			for (int i = 0; i < descLogs.logGroups().size(); i++) 
			{
				if (descLogs.logGroups().get(i).logGroupName().equalsIgnoreCase(logGroupName)) 
				{
					logArn = descLogs.logGroups().get(i).arn();
					applicationProperties.getAws().setLogArn(logArn);

					break;
				}
			}
		}
		Target target = Target.builder().arn(applicationProperties.getAws().getLogArn()).id(reservation_id).build();
		PutTargetsRequest targetRequest = PutTargetsRequest.builder().targets(target).rule(rule_name).build();
		PutTargetsResponse targetResponse = cwe.putTargets(targetRequest);

		return resultMap;
	}

	/**
	 * 인스턴스 시작
	 * 
	 * @param instance_id
	 * @return
	 */
	public Map<String, Object> startEC2(String instance_id) 
	{
		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

		DryRunSupportedRequest<StartInstancesRequest> dry_request = () -> 
		{
			StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instance_id);
			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) 
		{
			System.out.printf("Failed dry run to start instance %s", instance_id);

			throw dry_response.getDryRunResponse();
		}

		StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instance_id);
		ec2.startInstances(request);

		System.out.printf("Successfully started instance %s", instance_id);

		resultMap.put("instance_id", instance_id);
		logger.debug("instance_id [{}]", instance_id);

		return resultMap;
	}

	/**
	 * 인스턴스 중지
	 * 
	 * @param instance_id
	 * @return
	 */
	public Map<String, Object> stopEC2(String instance_id) 
	{

		AmazonEC2 ec2 = ec2Client();
		DryRunSupportedRequest<StopInstancesRequest> dry_request = () -> 
		{
			StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) 
		{
			System.out.printf("Failed dry run to stop instance %s", instance_id);
			throw dry_response.getDryRunResponse();
		}

		StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instance_id);

		ec2.stopInstances(request);

		System.out.printf("Successfully stop instance %s", instance_id);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("instance_id", instance_id);

		logger.debug("instance_id [{}]", instance_id);

		return resultMap;
	}

	/**
	 * 인스턴스 종료
	 * 
	 * @param instance_id
	 * @return
	 */
	public Map<String, Object> terminateEC2(String instance_id) 
	{
		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

		DryRunSupportedRequest<TerminateInstancesRequest> dry_request = () -> 
		{
			TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		DryRunResult dry_response = ec2.dryRun(dry_request);

		if (!dry_response.isSuccessful()) 
		{
			System.out.printf("Failed dry run to start instance %s", instance_id);

			throw dry_response.getDryRunResponse();
		}

		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instance_id);

		ec2.terminateInstances(request);

		System.out.printf("Successfully terminated instance %s", instance_id);

		resultMap.put("instance_id", instance_id);
		logger.debug("instance_id [{}]", instance_id);

		return resultMap;
	}

	/**
	 * 
	 * 인스턴스의 정보 출력
	 * 
	 * @return
	 */
	public Map<String, Object> descEC2(String instance_id) 
	{

		Map<String, Object> resultMap = new HashMap<>();

		AmazonEC2 ec2 = ec2Client();

		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds(instance_id);

		while (!done) 
		{

			DescribeInstancesResult response = ec2.describeInstances(request);
			for (Reservation reservation : response.getReservations()) 
			{
				for (Instance instance : reservation.getInstances()) 
				{
					resultMap.put("instance_id", instance.getInstanceId());
					resultMap.put("ami", instance.getImageId());
					resultMap.put("state", instance.getState().getName());
					resultMap.put("type", instance.getInstanceType());
					resultMap.put("monitoring_state", instance.getMonitoring().getState());
					resultMap.put("launchTime", instance.getLaunchTime());
					resultMap.put("public_DNS", instance.getPublicDnsName());
					resultMap.put("stateTransition", instance.getStateTransitionReason());

					// 모니터링을 위해 런티차임 저장
					applicationProperties.getAws().setStartTime(instance.getLaunchTime());

					logger.debug("descEc2 parameters [{}, {}, {}, {}, {}, {}, {}]", instance.getInstanceId(),
							instance.getImageId(), instance.getState().getName(), instance.getInstanceType(),
							instance.getMonitoring().getState(), instance.getLaunchTime(), instance.getPublicDnsName());
				}
			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) 
			{
				done = true;
			}
		}

		return resultMap;

	}

	/**
	 * 현재 확인할 수 있는 지표 목록 출력
	 * 
	 * @param instance_id
	 * @return
	 */
	public ArrayList<Object> monitoringList(String instance_id) 
	{

		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(applicationProperties.getAws().getAccessKeyId(),
				applicationProperties.getAws().getAccessKeySecret());

		final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.standard()
				.withRegion(applicationProperties.getAws().getRegion())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		System.out.println("Successfully put CloudWatch event");

		// 현재 선택한 인스턴스로 필터링
		DimensionFilter dimensions = new DimensionFilter();
		dimensions.setName("InstanceId");
		dimensions.setValue(instance_id);

		// 해당 인스턴스의 확인할 수 있는 지표명 리스트
		ListMetricsRequest requestMetricLst = new ListMetricsRequest().withNamespace("AWS/EC2")
				.withDimensions(dimensions);

		boolean flag = false;
		while (!flag) 
		{
			ListMetricsResult response = cw.listMetrics(requestMetricLst);

			for (Metric metric : response.getMetrics()) 
			{

				Map<String, Object> resultMap = new HashMap<>();
				resultMap.put("metricName", metric.getMetricName());
				resultList.add(i, resultMap);
				i++;
			}
			requestMetricLst.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) 
			{		
				flag = true;
			}
		}
		return resultList;
	}

	/**
	 * 선택한 지표명으로 해당 인스턴스의 지표 정보 출력
	 * 
	 * @param instance_id
	 * @param metricName
	 * @return
	 */
	public ArrayList<Object> monitoringDesc(String instance_id, String metricName) 
	{

		ArrayList<Object> resultList = new ArrayList<>();
		
		// 함수를 호출했을 때의 시간을 endTime으로, startTime( = 인스턴스 시작시간) 부터 현재 시간까지의 정보
		Date endTime = new Date();

		// 지표 데이터를 가져올 간격( 기본값은 최소 5분, 세부 모니터링 활성화 시 1분 까지 가능)
		Integer integer = new Integer(300);

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(applicationProperties.getAws().getAccessKeyId(),
				applicationProperties.getAws().getAccessKeySecret());

		final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.standard()
				.withRegion(applicationProperties.getAws().getRegion())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		// 인스턴스 id로 필터링
		Dimension filter = new Dimension();
		filter.setName("InstanceId");
		filter.setValue(instance_id);

		//메트릭데이터 요청 
		GetMetricDataRequest md = new GetMetricDataRequest().withEndTime(endTime)
				.withStartTime(applicationProperties.getAws().getStartTime()).withMetricDataQueries();

		// AWS/EC2 에서 해당 인스턴스 아이디에서 해당 지표명을 가져옴.
		Metric metric = new Metric().withNamespace("AWS/EC2").withDimensions(filter).withMetricName(metricName);
		
		// 앞서 구한 해당 지표명과 함께 해당 시간 간격(기본 5분)의 평균값을 가져옴
		MetricStat metricStat = new MetricStat().withMetric(metric).withPeriod(integer).withStat("Average");

		// id는 임의 값
		MetricDataQuery metricDataQuery = new MetricDataQuery().withId("m1").withMetricStat(metricStat);

		md.withMetricDataQueries(metricDataQuery);

		GetMetricDataResult rms = cw.getMetricData(md);


		// 해당 시간과 해당 값을 넘겨줌
		for (int i = 0; i < rms.getMetricDataResults().get(0).getTimestamps().size(); i++) 
		{
			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put("values", rms.getMetricDataResults().get(0).getValues()
					.get(rms.getMetricDataResults().get(0).getTimestamps().size() - (1 + i)));

			resultMap.put("time", rms.getMetricDataResults().get(0).getTimestamps()
					.get(rms.getMetricDataResults().get(0).getTimestamps().size() - (1 + i)));
			
		}

		return resultList;
	}

	/**
	 * 인스턴스의 이벤트 정도 로그 확인 
	 * @param instance_id
	 * @return
	 */
	public ArrayList<Object> logEC2(String instance_id) 
	{
		ArrayList<Object> resultList = new ArrayList<>();

		
		String rule_name = instance_id; //규칙명
		String logGroupName = "/aws/events/" + instance_id; //로그그룹명
		String accessKeyId = applicationProperties.getAws().getAccessKeyId(); 
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, accessKeySecret);

		CloudWatchEventsClient cwe = CloudWatchEventsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).build();
		CloudWatchLogsClient cwl = CloudWatchLogsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(Region.AP_NORTHEAST_2).build();

		// 로그그룹 생성 => 인스턴스 id로 생성
		// 매개변수 instance_id 를 eventPattern 에 넣기
		// state를 모두 확인할지, 받아서 확인할지 체크
		String eventPattern = "{\r\n" + "  \"source\": [\r\n" + "    \"aws.ec2\"\r\n" + "  ],\r\n"
				+ "  \"detail-type\": [\r\n" + "    \"EC2 Instance State-change Notification\"\r\n" + "  ],\r\n"
				+ "  \"detail\": {\r\n" + "    \"state\": [\r\n" + "      \"stopped\"\r\n" + "    ],\r\n"
				+ "    \"instance-id\": [\r\n" + "      \"" + instance_id + "\"\r\n" + "    ]\r\n" + "  }\r\n" + "}";

		try 
		{
			//규칙 생성 
			PutRuleRequest ruleRequest = PutRuleRequest.builder().name(rule_name).state(RuleState.ENABLED)
					.eventPattern(eventPattern).build();

			PutRuleResponse ruleResponse = cwe.putRule(ruleRequest);
			System.out.println(
					"Successfully created CloudWatch events rule " + rule_name + " with arn " + ruleResponse.ruleArn());

		} catch (ResourceAlreadyExistsException expected) 
		{
			// 규칙이 이미 있다면 추가할 필요 없음
			System.out.println("Log Events Rule already exists");
			// Ignored or expected.
		}

		try 
		{
			//로그그룹 생성 
			CreateLogGroupRequest logRequest = CreateLogGroupRequest.builder().logGroupName(logGroupName).build();
			CreateLogGroupResponse logResponse = cwl.createLogGroup(logRequest);

			System.out.println("Successfully create CloudWatch log Groups " + logResponse);

		} catch (software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException expected) 
		{
			// 로그 그룹이 있을 시에는 생성할 필요 없으므로 예외처리
			System.out.println("Log Events already exists");
			// Ignored or expected.
		} finally 
		{

			// 로그 데이터를 가져올 때 arn이 필요하므로 arn 저장
			DescribeLogGroupsResponse descLogs = cwl.describeLogGroups();
			for (int i = 0; i < descLogs.logGroups().size(); i++) 
			{
				if (descLogs.logGroups().get(i).logGroupName().equalsIgnoreCase(logGroupName)) 
				{
					applicationProperties.getAws().setLogArn(descLogs.logGroups().get(i).arn());
					break;
				}
			}
		}

		try 
		{
			// 앞서 생성한 규칙으로 타겟(로그그룹)과 연결
			// logArn은 위에서 가져온 로그 그룹의 arn
			Target target = Target.builder().arn(applicationProperties.getAws().getLogArn()).id(instance_id).build();
			PutTargetsRequest targetRequest = PutTargetsRequest.builder().targets(target).rule(rule_name).build();
			PutTargetsResponse targetResponse = cwe.putTargets(targetRequest);

			// 로그그룹 내의 모든 로그 스트림의 정보
			FilterLogEventsRequest filterLogEventsRequest = FilterLogEventsRequest.builder().logGroupName(logGroupName)
					.build();

			
			int logLimits = cwl.filterLogEvents(filterLogEventsRequest).events().size();
			for (int i = 0; i < logLimits; i++) 
			{

				//System.out.println(cwl.filterLogEvents(filterLogEventsRequest).events().get(i).message());

				//이 샘플에서는 인스턴스의 정확한 정지 시간만을 알기 위해 해당 데이터에서 시간 값만 가져옴 
				Date time = new Date(cwl.filterLogEvents(filterLogEventsRequest).events().get(i).timestamp());

				Map<String, Object> resultMap = new HashMap<>();

				resultMap.put("log", time);

				resultList.add(i, resultMap);

			}

		
		} catch (Exception e) 
		{
			System.out.println(e);
		}
		/**
		 * 위에서 처리한 로그 데이터 리턴
		 */
		return resultList;
	}

	

	
	/**
	 * CostExplorer를 사요하기 위해서는 정책을 만들어줘야함. 기본으로 제공해주는 샘플이 아니기 떄문에 콘솔에서 정책 생성 후 Cost Explorer Service 권한 추가 후 현재 자격 증명 파일에 정책 추가 
	 * @param filter = 필터링 값
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public ArrayList<Object> cost(String filter, String startDate, String endDate) 
	{
		
		ArrayList<Object> costsResult = new ArrayList<>();
		
		Map<String, Object> costsMap = new HashMap<>();

		Map<String, Object> filters = new HashMap<>();
		ArrayList<Object> metricsAndUsage = new ArrayList<>();

		Expression expression = new Expression();
		DimensionValues dimensions = new DimensionValues();

		//CostAndUsage 요청 ( TimePeriod 필수 ) , 
		// 일별, 혼합비용 , 서비스로 그룹화하여 요청 
		final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
				.withTimePeriod(new DateInterval().withStart(startDate).withEnd(endDate))
				.withGranularity(Granularity.DAILY).withMetrics("BLENDED_COST")
				.withGroupBy(new GroupDefinition().withType("DIMENSION").withKey("SERVICE"));

		
		//필터링이 있을 시 처리 서비스를 필터 값으로 필터링하여 데이터 필터링
		if (!(filter.equalsIgnoreCase("all"))) 
		{
			//필터값이 예를 들어 Cloud Watch일 시 , 서비스 목록에서 CloudWatch로 필터링하여 
			//해당 서비스의 요금만을 나타낼 수 있다.
			dimensions.withKey(com.amazonaws.services.costexplorer.model.Dimension.SERVICE);
			dimensions.withValues(filter);
			
			expression.withDimensions(dimensions);
			
			awsCERequest.withFilter(expression);
		}
		
		try 
		{
			//CostExplorer 생성자에 사용 할 자격증명 생성 현재 저장되어 있는 ID,Secret 사용
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(applicationProperties.getAws().getAccessKeyId(),
					applicationProperties.getAws().getAccessKeySecret());
			
			
			AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withRegion("ap-northeast-2")
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
			
			// 현재 Dimension (SERVICE) 의 값들 추가 
			// 즉 현재 지정한 날짜에 사용된 데이터들의 키 값( e.g. EC2, Cloud Watch)을 가져옴
			GetDimensionValuesRequest diRequest = new GetDimensionValuesRequest()
					.withTimePeriod(new DateInterval().withStart(startDate).withEnd(endDate)).withDimension("SERVICE");
			
			GetDimensionValuesResult result = ce.getDimensionValues(diRequest);
			
			ArrayList<Object> filterValues = new ArrayList<>();

			
			//필터링 할 수 있는 값들을 리스트에 추가 
			result.getDimensionValues().forEach(action -> 
			{
				filterValues.add(action.getValue());
			});


			boolean done = false;

			String targetAmount = "Amount";
			String targetUnit = "Unit";

			while (!done) 
			{
				
				//CostAndUsage 요청의 결과를 받음 
				GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);
				
				//타임 별로 데이터 확인( 일 별로 지정 했기 때문에 일별로 사용한 데이터들의 값들을 볼 수 있음)
				for (ResultByTime resultByTime : ceResult.getResultsByTime()) 
				{
					ArrayList<Object> metric = new ArrayList<>();
					Map<String, Object> metricData = new HashMap<>();

					double sum = 0;
	
					
					
					// 일별로 데이터를 확인하며 그룹안에 있는 데이터 확인 
					// 서비스별로 그룹화하였기 떄문에 서비스를 키 값으로, 메트릭은 사용금액과 화폐 단위를 보여준다.
					// e.g. Groups : [{Keys: [AWS Glue],Metrics: {BlendedCost={Amount: 0,Unit: USD}}}]
					for (Group groups : resultByTime.getGroups()) 
					{

						Map<String, Object> metricKeysAndAmount = new HashMap<>();
						// e.g. Metrics = [{ amount : 0, keys=[TAX]} 
						// Metrics는 위와 같은 형식의 데이터 이므로 해당 값을 가져오기 위하여 values 사용 
						String metrics = groups.getMetrics().values().toString();

						
						//데이터를 넘기기 위해 데이터 처리 부분 
						
						// amount 값만 저장
						String amount = metrics.substring(metrics.indexOf("Amount") + targetAmount.length() + 2,
								metrics.indexOf("Unit") - 1);

						// unit 값만 저장
						// String unit = metrics.substring(metrics.indexOf("Unit") + targetUnit.length()
						// + 2,metrics.length() - 2);

						// e.g. resultMap : [{amount = 0, keys = AWS Glue}]
						metricKeysAndAmount.put("amount", amount);
						metricKeysAndAmount.put("key",
								groups.getKeys().toString().substring(1, groups.getKeys().toString().length() - 1));

						//서비스 별로 금액을 모두 더하여 sum으로 저장
						sum += Double.parseDouble(amount);

						metric.add(metricKeysAndAmount);

					}

					//metricData에는 각 일별, 어떤 서비르를 사용했는지, 일별 총 금액을 넣어준다.
					metricData.put("date", resultByTime.getTimePeriod().getStart());
					metricData.put("metrics", metric);
					metricData.put("total", String.format("%.3f", sum));
					metricsAndUsage.add(metricData);

				}
				//더이상 데이터가 없을 시 종료
				awsCERequest.setNextPageToken(ceResult.getNextPageToken());	
				
				if (ceResult.getNextPageToken() == null) 
				{
					done = true;
				}
			}
			
			//앞서 구한 필터링할 수 있는 데이터들을 저장
			filters.put("filters", filterValues);
			
			costsMap.put("filterList", filters);

			costsMap.put("metricUsage", metricsAndUsage);

			//값을 넘겨줄 떄에는 리스트 안에 들어있는 두개의 맵 (필터 리스트와 사용량 리스트) 리턴
			costsResult.add(costsMap);
			
		} catch (final Exception e) {
			System.out.println(e);
		}
		return costsResult;
	}
	
	
}