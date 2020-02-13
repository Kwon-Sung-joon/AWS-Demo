package AWS;

import java.util.Date;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * Lists CloudWatch metrics
 */
public class MonitoringTests {

	public static void main(String[] args) {

		AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		boolean done = false;
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.withInstanceIds("i-04f7cf4f148fb7544");

		Date startTime = new Date();
		// 런치타임부터 언제까지 구할지 시간을 정해놓음
		Date endTime = new Date(startTime.getTime());
		endTime.setMinutes(5);

		final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

		// 인스턴스 아이디로 정보 가져오기
		Dimension filter = new Dimension();
		filter.setName("InstanceId");
		filter.setValue("i-04f7cf4f148fb7544");

		while (!done) {

			DescribeInstancesResult response = ec2.describeInstances(request);
			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {

					startTime = instance.getLaunchTime();

				}
				// period min = 60s
				// https://docs.aws.amazon.com/ko_kr/AWSEC2/latest/UserGuide/using-cloudwatch-new.html
				// 최소 5분, 세부 모니터링 활성화 시 1분 단위
				
				// MetricData 요청 -> (시작,종료시간, 데이터쿼리 필수)  -> 지표 불러오기 -> 지표 설정 -> 지표 데이터 쿼리 -> 지표 렬과
				
				Integer integer = new Integer(60);

				Date now = new Date();

				GetMetricDataRequest md = new GetMetricDataRequest().withEndTime(now).withStartTime(startTime)
						.withMetricDataQueries();

				Metric metric = new Metric().withNamespace("AWS/EC2").withDimensions(filter)
						.withMetricName("NetworkOut");

				MetricStat metricStat = new MetricStat().withMetric(metric).withPeriod(integer).withStat("Average");

				MetricDataQuery metricDataQuery = new MetricDataQuery().withId("m1").withMetricStat(metricStat);
				md.withMetricDataQueries(metricDataQuery);

				GetMetricDataResult rms = cw.getMetricData(md);
				// rms.getMetricDataResults();
				System.out.println(rms.getMetricDataResults());

			}

			request.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				done = true;
			}
		}
		
		//지표명 불러오기 -> 지표 목록 요청 (네임 스페이스, 필터) -> 지표 목록 출력
		DimensionFilter dimensions = new DimensionFilter();
		dimensions.setName("InstanceId");
		dimensions.setValue("i-04f7cf4f148fb7544");
		ListMetricsRequest requestMetricLst = new ListMetricsRequest().withNamespace("AWS/EC2")
				.withDimensions(dimensions);
		boolean flag = false;
		while (!flag) {
			ListMetricsResult response = cw.listMetrics(requestMetricLst);

			for (Metric metric : response.getMetrics()) {
				System.out.println("Retrieved metric" + metric.getMetricName());
			}
			requestMetricLst.setNextToken(response.getNextToken());

			if (response.getNextToken() == null) {
				flag = true;
			}
		}

	}
}