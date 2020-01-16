package kr.co.starlabs.cloudAWS;

import java.util.UUID;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;

/**
 * Creates an EC2 instance
 */
public class CreateInstanceTests {
	public static void main(String[] args) {


		String name = "d1jd9281";
		String ami_id = "ami-0bea7fd38fabe821a";

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		RunInstancesRequest run_request = new RunInstancesRequest().withImageId(ami_id)
				.withInstanceType(InstanceType.T1Micro).withMaxCount(1).withMinCount(1);

		RunInstancesResult run_response = ec2.runInstances(run_request);

		String reservation_id = run_response.getReservation().getInstances().get(0).getInstanceId();

		Tag tag = new Tag().withKey("Name").withValue(name);

		CreateTagsRequest tag_request = new CreateTagsRequest().withTags(tag);

		CreateTagsResult tag_response = ec2.createTags(tag_request);

		System.out.printf("Successfully started EC2 instance %s based on AMI %s", reservation_id, ami_id);
	}
}