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
import com.amazonaws.services.costexplorer.model.GetDimensionValuesRequest;
import com.amazonaws.services.costexplorer.model.GetDimensionValuesResult;
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
		ArrayList<Object> costs = new ArrayList<>();
		ArrayList<Object> keyValues = new ArrayList<>();
		Expression expression = new Expression();
		DimensionValues dimensions = new DimensionValues();

		System.out.println(dimensions.getKey());

		final GetCostAndUsageRequest awsCERequest = new GetCostAndUsageRequest()
				.withTimePeriod(new DateInterval().withStart("2020-01-01").withEnd("2020-02-01"))
				.withGranularity(Granularity.DAILY).withMetrics("BlendedCost")// .withFilter(expression)
				.withGroupBy(new GroupDefinition().withType("DIMENSION").withKey("SERVICE"));

		// System.out.println(awsCERequest.getFilter().getDimensions().getValues());

	

		try {
			BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIA4ZXEMVK2ELZL7V72",
					"eun13hhmSRw0Xv2/bB+7iZw9rUOYNYKGwdS7gqpg");
			AWSCostExplorer ce = AWSCostExplorerClientBuilder.standard().withRegion("ap-northeast-2")
					.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
			
			

			//필터링 목록  리턴값에 추가 
			GetDimensionValuesRequest diRequest = new GetDimensionValuesRequest()
					.withTimePeriod(new DateInterval().withStart("2020-01-01").withEnd("2020-02-01")).withDimension("SERVICE");
			GetDimensionValuesResult result = ce.getDimensionValues(diRequest);

			ArrayList<Object> dimensionValues = new ArrayList<>();
			result.getDimensionValues().forEach(action->{
				dimensionValues.add(action.getValue());
			});
			Map<String, Object> dimensionValuesMap = new HashMap<>();
			dimensionValuesMap.put("dimensions", dimensionValues);
			
			
			boolean done = false;

			String targetAmount = "Amount";
			String targetUnit = "Unit";

			while (!done) {
				GetCostAndUsageResult ceResult = ce.getCostAndUsage(awsCERequest);
				
				for (ResultByTime resultByTime : ceResult.getResultsByTime()) {
					ArrayList<Object> resultList = new ArrayList<>();
					Map<String, Object> values = new HashMap<>();

					double sum = 0;

					// e.g. Groups : [{Keys: [AWS Glue],Metrics: {BlendedCost={Amount: 0,Unit:
					// USD}}}]
					for (Group groups : resultByTime.getGroups()) {
						
						Map<String, Object> resultMap = new HashMap<>();
						// e.g. Metrics = [{ amount : 0, keys=[TAX]}
						String metrics = groups.getMetrics().values().toString();

						// amount 값만 저장
						String amount = metrics.substring(metrics.indexOf("Amount") + targetAmount.length() + 2,
								metrics.indexOf("Unit") - 1);
						// unit 값만 저장
						// String unit = metrics.substring(metrics.indexOf("Unit") + targetUnit.length()
						// + 2,metrics.length() - 2);

						// e.g. resultMap : [{amount = 0, keys = AWS Glue}]
						resultMap.put("amount", amount);
						resultMap.put("keys",
								groups.getKeys().toString().substring(1, groups.getKeys().toString().length() - 1));

						sum += Double.parseDouble(amount);

						resultList.add(resultMap);
						
					}
					
					values.put("date", resultByTime.getTimePeriod().getStart());
					values.put("metrics", resultList);
			
					values.put("total", String.format("%.3f", sum));

					keyValues.add(values);

				}
				
				awsCERequest.setNextPageToken(ceResult.getNextPageToken());

				if (ceResult.getNextPageToken() == null) {
					done = true;
				}
			}
			keyValues.add(dimensionValuesMap);
			System.out.println(keyValues);
			//keyValues.add(dimensionValuesMap);
			//costs.add(keyValues);
			System.out.println(costs);
		} catch (final Exception e) {
			System.out.println(e);
		}

	}
}