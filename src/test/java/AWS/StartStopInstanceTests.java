package AWS;

// snippet-start:[ec2.java2.start_stop_instance.complete]
// snippet-start:[ec2.java2.start_stop_instance.import]
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

// snippet-end:[ec2.java2.start_stop_instance.import]
/**
 * Starts or stops and EC2 instance
 */
public class StartStopInstanceTests {
	public static void startInstance(String instance_id) {
		
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

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
	}

	public static void stopInstance(String instance_id) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

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
	}

	public static void main(String[] args) {
		final String USAGE = "To run this example, supply an instance id and start or stop\n"
				+ "Ex: StartStopInstance <instance-id> <start|stop>\n";

		if (args.length != 1) {
			System.out.println(USAGE);
			//System.exit(1);
		}

		String instance_id = "i-05030a3f53293474f";

		boolean start;

//		if (args[1].equals("start")) {
//			
//		} else {
//			start = false;
//		}
		start=false;
		if (start) {
			startInstance(instance_id);
		} else {
			stopInstance(instance_id);
		}
	}
}