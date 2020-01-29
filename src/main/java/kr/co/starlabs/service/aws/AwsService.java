package kr.co.starlabs.service.aws;

import java.util.HashMap;

import java.util.Map;
import java.util.UUID;

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

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
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
import com.amazonaws.services.ec2.model.MonitorInstancesRequest;
import com.amazonaws.services.ec2.model.Monitoring;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.PutEventsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.cloudwatchevents.model.PutEventsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.RuleState;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import kr.co.starlabs.config.ApplicationProperties;
import kr.co.starlabs.controller.aws.AwsController;

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

	}

	/**
	 * ec2 클라이언트 생성자
	 * 
	 * @return
	 */
	public AmazonEC2 client() {

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

		AmazonEC2 ec2 = client();

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

		AmazonEC2 ec2 = client();
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

		AmazonEC2 ec2 = client();

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

		AmazonEC2 ec2 = client();
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

		// stopping이 아닌, stopped의 시간
		Date origin = null;

		while (ec2.stopInstances(request).getStoppingInstances().get(0).getCurrentState().getCode() != 80) {

			Date stopTime = new Date();
			// 인스턴스가 완전히 중지한 시간을 구함
			origin = stopTime;
		}
		if (origin != null) {
			applicationProperties.getAws().setStopTime(origin);
		}
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

		AmazonEC2 ec2 = client();

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

		AmazonEC2 ec2 = client();

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
					resultMap.put("stopTime", applicationProperties.getAws().getStopTime());

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

	public ArrayList<Object> monitoringDesc(String instance_id, String metricName) {

		ArrayList<Object> resultList = new ArrayList<>();

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

		GetMetricDataRequest md = new GetMetricDataRequest().withEndTime(endTime)
				.withStartTime(applicationProperties.getAws().getStartTime()).withMetricDataQueries();

		Metric metric = new Metric().withNamespace("AWS/EC2").withDimensions(filter).withMetricName(metricName);

		MetricStat metricStat = new MetricStat().withMetric(metric).withPeriod(integer).withStat("Average");

		MetricDataQuery metricDataQuery = new MetricDataQuery().withId("m1").withMetricStat(metricStat);
		md.withMetricDataQueries(metricDataQuery);

		GetMetricDataResult rms = cw.getMetricData(md);

		// System.out.println(rms.getMetricDataResults());

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

	public ArrayList<Object> monitoringList(String instance_id) {

		ArrayList<Object> resultList = new ArrayList<>();
		int i = 0;

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(applicationProperties.getAws().getAccessKeyId(),
				applicationProperties.getAws().getAccessKeySecret());

		final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.standard()
				.withRegion(applicationProperties.getAws().getRegion())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		System.out.println("Successfully put CloudWatch event");

		DimensionFilter dimensions = new DimensionFilter();
		dimensions.setName("InstanceId");
		dimensions.setValue(instance_id);
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

	public Map<String, Object> logEC2(String instance_id) {
		// CloudWatch 규칙생성 및 로그그룹 연결
		// IAM 유저에 CloudWatchLogsFullAccess, CloudWatchEventsFullAccess 정책 추가 필요
		CloudWatchEventsClient cwe = CloudWatchEventsClient.builder().build();

		// 이벤트 규칙 이름 생성
		String rule_name = "ec2_state_rule_" + UUID.randomUUID().toString();

		// 이벤트 패턴 JSON 형식
		// 매개변수 instance_id 를 eventPattern 에 넣기
		String eventPattern = "{\r\n" + "  \"source\": [\r\n" + "    \"aws.ec2\"\r\n" + "  ],\r\n"
				+ "  \"detail-type\": [\r\n" + "    \"EC2 Instance State-change Notification\"\r\n" + "  ],\r\n"
				+ "  \"detail\": {\r\n" + "    \"state\": [\r\n" + "      \"stopped\",\r\n" + "      \"running\",\r\n"
				+ "      \"terminated\"\r\n" + "    ],\r\n" + "    \"instance-id\": [\r\n"
				+ "      \"i-07b8ff5494d0ff5e7\"\r\n" + "    ]\r\n" + "  }\r\n" + "}";

		PutRuleRequest ruleRequest = PutRuleRequest.builder().name(rule_name).state(RuleState.ENABLED)
				.eventPattern(eventPattern).build();

		PutRuleResponse ruleResponse = cwe.putRule(ruleRequest);

		System.out.printf("Successfully created CloudWatch events rule %s with arn %s", rule_name,
				ruleResponse.ruleArn());

		// arn = 로그그룹 arn id= ??
		Target target = Target.builder().arn("arn:aws:logs:ap-northeast-2:672956273056:log-group:/aws/events/logs:*")
				.id("asd").build();

		// 앞서 생성한 규칙으로 타겟 생성
		PutTargetsRequest request1 = PutTargetsRequest.builder().targets(target).rule(rule_name).build();

		PutTargetsResponse response = cwe.putTargets(request1);
		// snippet-end:[cloudwatch.java2.put_targets.main]

		System.out.println("Successfully created CloudWatch events target for rule " + rule_name);

		CloudWatchLogsClient cloudWatchLogsClient = CloudWatchLogsClient.builder().region(Region.AP_NORTHEAST_2)
				.build();
		// 로그 이벤트 가져오기 ==> 데이터 확인
		GetLogEventsRequest getLogEventsRequest = GetLogEventsRequest.builder().logGroupName("/aws/events/logs")
				.logStreamName("asd").startFromHead(true).build();

		int logLimit = cloudWatchLogsClient.getLogEvents(getLogEventsRequest).events().size();
		for (int c = 0; c < logLimit; c++) {
			// Prints the messages to the console
			System.out.println(cloudWatchLogsClient.getLogEvents(getLogEventsRequest).events().get(c).message());
		}
		System.out.println("Successfully got CloudWatch log events!");

		/**
		 * 로그 데이터 리턴
		 */
		return null;
	}
}
