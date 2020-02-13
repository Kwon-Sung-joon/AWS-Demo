//package kr.co.starlabs.cloudAWS;
//
//public class qwd {
//	GetMetricDataRequest getMetricDataRequest = new GetMetricDataRequest().withMetricDataQueries();
//    Integer integer = new Integer(300);
//
//    Iterator<Map.Entry<String, String>> entries = dimensions.entrySet().iterator();
//    List<Dimension> dList = new ArrayList<Dimension>();
//    while (entries.hasNext()) {
//        Map.Entry<String, String> entry = entries.next();
//        dList.add(new Dimension().withName(entry.getKey()).withValue(entry.getValue()));
//    }
//
//    com.amazonaws.services.cloudwatch.model.Metric metric = new com.amazonaws.services.cloudwatch.model.Metric();
//    metric.setNamespace(namespace);
//    metric.setMetricName(metricName);
//    metric.setDimensions(dList);
//    MetricStat ms = new MetricStat().withMetric(metric)
//                                    .withPeriod(integer)
//                                    .withUnit(StandardUnit.None)
//                                    .withStat("Average");
//    MetricDataQuery metricDataQuery = new MetricDataQuery().withMetricStat(ms)
//                                                            .withId("m1");
//
//    List<MetricDataQuery> mqList = new ArrayList<>();
//    mqList.add(metricDataQuery);
//    getMetricDataRequest.withMetricDataQueries(mqList);
//    long timestamp = 1536962700000L;
//    long timestampEnd = 1536963000000L;
//      Date d = new Date(timestamp );
//      Date dEnd = new Date(timestampEnd );
//    getMetricDataRequest.withStartTime(d);
//    getMetricDataRequest.withEndTime(dEnd);
//    GetMetricDataResult result1=  cw.getMetricData(getMetricDataRequest);
//}
