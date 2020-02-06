package kr.co.starlabs.cloudAWS;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
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
import com.amazonaws.services.costexplorer.model.ResultByTime;

import static org.junit.jupiter.api.Assertions.assertAll;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BillingTest {
	public static void main(String args[]) {

		Expression expression = new Expression();
		DimensionValues dimensions = new DimensionValues();

		dimensions.withKey(com.amazonaws.services.costexplorer.model.Dimension.SERVICE);
		dimensions.withValues("EC2");
		expression.withDimensions(dimensions);

		final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
				.withTimePeriod(new DateInterval().withStart("2020-01-31").withEnd("2020-02-03"))
				.withGranularity(Granularity.DAILY).withMetrics("BlendedCost")// .withFilter(expression)
				.withGroupBy(new GroupDefinition().withType("DIMENSION").withKey("SERVICE"));

		try {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIAZZL2R2GQNGLEYS5Z",
					"EthXkcgBGIKRYYKzk99lHknVGS3I0sW8ar617gu5");
			AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withRegion("ap-northeast-2")
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
			boolean done = false;
			int i = 0;

			
			String target = "Amount";
			String target2 = "Unit";
			ArrayList<Object> resultList = new ArrayList<>();
			while (!done) {
				GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);

				for (ResultByTime resultByTime : ceResult.getResultsByTime()) {

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

						//e.g. resultMap : [{amount = 0, keys = AWS Glue}]
						resultMap.put("amount", amount);
						resultMap.put("keys", groups.getKeys().toString().substring(1, groups.getKeys().toString().length()-1));

						sum += Double.parseDouble(amount);

						System.out.println(resultMap);
						resultList.add(i,resultMap);
						i++;
					}

					System.out.println();
					System.out.println(resultList);
					System.out.println();
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

	}
}