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
import com.amazonaws.services.costexplorer.model.Granularity;
import com.amazonaws.services.costexplorer.model.Group;
import com.amazonaws.services.costexplorer.model.GroupDefinition;
import com.amazonaws.services.costexplorer.model.MetricValue;
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
import com.amazonaws.services.identitymanagement.model.DetachUserPolicyResult;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;

import com.amazonaws.services.logs.model.ResourceAlreadyExistsException;

import java.util.Date;
import java.util.ArrayList;
import java.util.Collection;
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
//import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
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
public class AwsService {

	private static final Logger logger = LoggerFactory.getLogger(AwsService.class);

	@Autowired
	private ApplicationProperties applicationProperties;

	/**
	 * 새로운 IAM 유저 생성
	 * 
	 * @param username
	 * @return
	 */

	public Map<String, Object> createUser(String username) {

		Map<String, Object> resultMap = new HashMap<>();

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

		// 정책 2개 추가 데모.
		AttachUserPolicyRequest attach_request2 = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchLogsFullAccess");

		AttachUserPolicyRequest attach_request3 = new AttachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchEventsFullAccess");



		// Cloud Watch Logs, Events 정책 추가

		iam.attachUserPolicy(attach_request);
		iam.attachUserPolicy(attach_request2);
		iam.attachUserPolicy(attach_request3);

		System.out.println("Successfully attached policy " + policy_arn + " to user " + username);

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
	public void logout() {
		String username = applicationProperties.getAws().getUsername();
		String policy_arn = applicationProperties.getAws().getPolicy_arn();
		String access_key = applicationProperties.getAws().getAccessKeyId();

		/*
		 * 유저 삭제 순서 : 정책 제거 -> 액세스 키 제거 -> 유저 삭제
		 */
		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		DetachUserPolicyRequest requestDetachUserPolicy = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn(policy_arn);

		DetachUserPolicyRequest requestDetachUserPolicy2 = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchLogsFullAccess");

		DetachUserPolicyRequest requestDetachUserPolicy3 = new DetachUserPolicyRequest().withUserName(username)
				.withPolicyArn("arn:aws:iam::aws:policy/CloudWatchEventsFullAccess");

		iam.detachUserPolicy(requestDetachUserPolicy);
		iam.detachUserPolicy(requestDetachUserPolicy2);
		iam.detachUserPolicy(requestDetachUserPolicy3);

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
	 * 
	 * 
	 * @return
	 */

	public ArrayList<Object> listEC2() {
		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		AmazonEC2 ec2 = ec2Client();

		DescribeInstancesRequest request = new DescribeInstancesRequest();
		boolean done = false;

		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {

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

			if (response.getNextToken() == null) {
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
	public Map<String, Object> createEC2(String name) {

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

		try {
			PutRuleRequest ruleRequest = PutRuleRequest.builder().name(rule_name).state(RuleState.ENABLED)
					.eventPattern(eventPattern).build();

			PutRuleResponse ruleResponse = cwe.putRule(ruleRequest);
			System.out.println(
					"Successfully created CloudWatch events rule " + rule_name + " with arn " + ruleResponse.ruleArn());

		} catch (ResourceAlreadyExistsException expected) {
			// 규칙이 이미 있다면 추가할 필요 없음
			System.out.println("Log Events Rule already exists");
			// Ignored or expected.
		}

		try {

			CreateLogGroupRequest logRequest = CreateLogGroupRequest.builder().logGroupName(logGroupName).build();
			CreateLogGroupResponse logResponse = cwl.createLogGroup(logRequest);

			System.out.println("Successfully create CloudWatch log Groups " + logResponse);

		} catch (software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException expected) {
			// 로그 그룹이 있을 시에는 생성할 필요 없으므로 예외처리
			System.out.println("Log Events already exists");
			// Ignored or expected.
		} finally {

			// 로그 데이터를 가져올 때 arn이 필요하므로 arn 가져오기
			DescribeLogGroupsResponse descLogs = cwl.describeLogGroups();
			for (int i = 0; i < descLogs.logGroups().size(); i++) {
				if (descLogs.logGroups().get(i).logGroupName().equalsIgnoreCase(logGroupName)) {
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
		logger.debug("instance_id [{}]", instance_id);

		return resultMap;
	}

	/**
	 * 인스턴스 중지
	 * 
	 * @param instance_id
	 * @return
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

		logger.debug("instance_id [{}]", instance_id);

		return resultMap;
	}

	/**
	 * 인스턴스 종료
	 * 
	 * @param instance_id
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
		logger.debug("instance_id [{}]", instance_id);

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
		request.withInstanceIds(instance_id);

		while (!done) {

			DescribeInstancesResult response = ec2.describeInstances(request);
			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
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

			if (response.getNextToken() == null) {
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
	public ArrayList<Object> monitoringList(String instance_id) {

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
		while (!flag) {
			ListMetricsResult response = cw.listMetrics(requestMetricLst);

			for (Metric metric : response.getMetrics()) {

				Map<String, Object> resultMap = new HashMap<>();
				resultMap.put("metricName", metric.getMetricName());
				resultList.add(i, resultMap);
				i++;
			}
			requestMetricLst.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
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
	public ArrayList<Object> monitoringDesc(String instance_id, String metricName) {

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

		// startTime Demo.
		// Date startTime = new Date(endTime.getTime());
		// startTime.setMinutes(50);

		GetMetricDataRequest md = new GetMetricDataRequest().withEndTime(endTime)
				.withStartTime(applicationProperties.getAws().getStartTime()).withMetricDataQueries();

		// 해당 지표를 가져옴
		Metric metric = new Metric().withNamespace("AWS/EC2").withDimensions(filter).withMetricName(metricName);

		// 해당 지표의 값을 가죠온다.
		MetricStat metricStat = new MetricStat().withMetric(metric).withPeriod(integer).withStat("Average");

		// id는 임의 값
		MetricDataQuery metricDataQuery = new MetricDataQuery().withId("m1").withMetricStat(metricStat);

		md.withMetricDataQueries(metricDataQuery);

		GetMetricDataResult rms = cw.getMetricData(md);

		// System.out.println(rms.getMetricDataResults());

		// 해당 시간과 해당 값을 넘겨줌
		for (int i = 0; i < rms.getMetricDataResults().get(0).getTimestamps().size(); i++) {
			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put("values", rms.getMetricDataResults().get(0).getValues()
					.get(rms.getMetricDataResults().get(0).getTimestamps().size() - (1 + i)));

			resultMap.put("time", rms.getMetricDataResults().get(0).getTimestamps()
					.get(rms.getMetricDataResults().get(0).getTimestamps().size() - (1 + i)));

			resultList.add(i, resultMap);
		}

		return resultList;
	}

	public ArrayList<Object> logEC2(String instance_id) {
		ArrayList<Object> resultList = new ArrayList<>();

		String rule_name = instance_id;
		String logGroupName = "/aws/events/" + instance_id;
		String accessKeyId = applicationProperties.getAws().getAccessKeyId();
		String accessKeySecret = applicationProperties.getAws().getAccessKeySecret();

		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, accessKeySecret);

		CloudWatchEventsClient cwe = CloudWatchEventsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).build();
		// 기본 루트 클라이언트로 생성하므로 수정 필요
		CloudWatchLogsClient cwl = CloudWatchLogsClient.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(Region.AP_NORTHEAST_2).build();

		// 로그그룹 생성 => 인스턴스 id로 생성

		// 매개변수 instance_id 를 eventPattern 에 넣기
		// state를 모두 확인할지, 받아서 확인할지 체크
		String eventPattern = "{\r\n" + "  \"source\": [\r\n" + "    \"aws.ec2\"\r\n" + "  ],\r\n"
				+ "  \"detail-type\": [\r\n" + "    \"EC2 Instance State-change Notification\"\r\n" + "  ],\r\n"
				+ "  \"detail\": {\r\n" + "    \"state\": [\r\n" + "      \"stopped\"\r\n" + "    ],\r\n"
				+ "    \"instance-id\": [\r\n" + "      \"" + instance_id + "\"\r\n" + "    ]\r\n" + "  }\r\n" + "}";

		try {
			PutRuleRequest ruleRequest = PutRuleRequest.builder().name(rule_name).state(RuleState.ENABLED)
					.eventPattern(eventPattern).build();

			PutRuleResponse ruleResponse = cwe.putRule(ruleRequest);
			System.out.println(
					"Successfully created CloudWatch events rule " + rule_name + " with arn " + ruleResponse.ruleArn());

		} catch (ResourceAlreadyExistsException expected) {
			// 규칙이 이미 있다면 추가할 필요 없음
			System.out.println("Log Events Rule already exists");
			// Ignored or expected.
		}

		try {

			CreateLogGroupRequest logRequest = CreateLogGroupRequest.builder().logGroupName(logGroupName).build();
			CreateLogGroupResponse logResponse = cwl.createLogGroup(logRequest);

			System.out.println("Successfully create CloudWatch log Groups " + logResponse);

		} catch (software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException expected) {
			// 로그 그룹이 있을 시에는 생성할 필요 없으므로 예외처리
			System.out.println("Log Events already exists");
			// Ignored or expected.
		} finally {

			// 로그 데이터를 가져올 때 arn이 필요하므로 arn 가져오기
			DescribeLogGroupsResponse descLogs = cwl.describeLogGroups();
			for (int i = 0; i < descLogs.logGroups().size(); i++) {
				if (descLogs.logGroups().get(i).logGroupName().equalsIgnoreCase(logGroupName)) {
					applicationProperties.getAws().setLogArn(descLogs.logGroups().get(i).arn());
					break;
				}
			}
		}

		// arn = 로그그룹 arn id= ??
		try {
			// 앞서 생성한 규칙으로 타겟(로그그룹)연결
			// logArn은 위에서 가져온 로그 그룹의 arn
			Target target = Target.builder().arn(applicationProperties.getAws().getLogArn()).id(instance_id).build();
			PutTargetsRequest targetRequest = PutTargetsRequest.builder().targets(target).rule(rule_name).build();
			PutTargetsResponse targetResponse = cwe.putTargets(targetRequest);

			// 로그그룹 내의 모든 로그 스트림의 정보
			FilterLogEventsRequest filterLogEventsRequest = FilterLogEventsRequest.builder().logGroupName(logGroupName)
					.build();

			int logLimits = cwl.filterLogEvents(filterLogEventsRequest).events().size();
			for (int i = 0; i < logLimits; i++) { // Prints the messages to the console

				System.out.println(cwl.filterLogEvents(filterLogEventsRequest).events().get(i).message());

				// System.out.println(cwl.filterLogEvents(filterLogEventsRequest).events().get(i).message().getClass());

				Date time = new Date(cwl.filterLogEvents(filterLogEventsRequest).events().get(i).timestamp());

				Map<String, Object> resultMap = new HashMap<>();

				resultMap.put("log", time);

				resultList.add(i, resultMap);

			}

		} catch (Exception e) {
			System.out.println(e);
		}
		/**
		 * 위에서 처리한 로그 데이터 리턴
		 */
		return resultList;
	}

	/**
	 * CostExplorer를 사요하기 위해서는 정책을 만들어줘야함. 콘솔에서 정책 생성 후 현재 자격증명파일에 정책 부여
	 * 
	 * @return
	 */
	public ArrayList<Object> cost() {
		ArrayList<Object> keyValues = new ArrayList<>();

		Expression expression = new Expression();
		DimensionValues dimensions = new DimensionValues();

		dimensions.withKey(com.amazonaws.services.costexplorer.model.Dimension.SERVICE);
		dimensions.withValues("EC2");
		expression.withDimensions(dimensions);

		final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
				.withTimePeriod(new DateInterval().withStart("2020-01-01").withEnd("2020-02-03"))
				.withGranularity(Granularity.DAILY).withMetrics("BlendedCost")// .withFilter(expression)
				.withGroupBy(new GroupDefinition().withType("DIMENSION").withKey("SERVICE"));

		try {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(applicationProperties.getAws().getAccessKeyId(),
					applicationProperties.getAws().getAccessKeySecret());
			AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withRegion("ap-northeast-2")
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
			boolean done = false;
			int i = 0;

			String target = "Amount";
			String target2 = "Unit";


			while (!done) {
				GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);

				for (ResultByTime resultByTime : ceResult.getResultsByTime()) {
					ArrayList<Object> resultList = new ArrayList<>();
					Map<String, Object> values = new HashMap<>();

					System.out.println(resultByTime.getTimePeriod().getStart());

					// resultList.add(i,resultByTime.getTimePeriod().getStart().toString());
					double sum = 0;

					for (Group groups : resultByTime.getGroups()) {
						// e.g. Groups : [{Keys: [AWS Glue],Metrics: {BlendedCost={Amount: 0,Unit:
						// USD}}}]

						Map<String, Object> resultMap = new HashMap<>();
						// e.g. Metrics = [{ amount : 0, keys=[TAX]}
						String metrics = groups.getMetrics().values().toString();

						// amount 값만 저장
						String amount = metrics.substring(metrics.indexOf("Amount") + target.length() + 2,
								metrics.indexOf("Unit") - 1);
						// unit 값만 저장
						String unit = metrics.substring(metrics.indexOf("Unit") + target2.length() + 2,
								metrics.length() - 2);

						// e.g. resultMap : [{amount = 0, keys = AWS Glue}]
						resultMap.put("amount", amount);
						resultMap.put("keys",
								groups.getKeys().toString().substring(1, groups.getKeys().toString().length() - 1));

						sum += Double.parseDouble(amount);
						
						System.out.println(resultMap);
						resultList.add(resultMap);
					}
					values.put("total",String.format("%.2f", sum));
					System.out.println();
					values.put("key", resultByTime.getTimePeriod().getStart());
					values.put("value", resultList);
					keyValues.add(i, values);
					// System.out.println(String.for mat("합계 : %.2f", sum));

				}

				awsCERequest.setNextPageToken(ceResult.getNextPageToken());

				if (ceResult.getNextPageToken() == null) {
					done = true;
				}
			}

		} catch (final Exception e) {
			System.out.println(e);
		}
		return keyValues;
	}
}
