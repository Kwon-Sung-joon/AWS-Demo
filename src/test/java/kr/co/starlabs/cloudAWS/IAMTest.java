package kr.co.starlabs.cloudAWS;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;

public class IAMTest {

    public static void main(String[] args) {

        final String USAGE =
            "To run this example, supply a username\n" +
            "Ex: CreateUser <username>\n";

        if (args.length != 1) {
            System.out.println(USAGE);
            //System.exit(1);
        }

        String username = "e3r3gwefwr";
        
        System.out.println(username);
        final AmazonIdentityManagement iam =
            AmazonIdentityManagementClientBuilder.defaultClient();

        CreateUserRequest request = new CreateUserRequest()
            .withUserName(username);

        CreateUserResult response = iam.createUser(request);

        System.out.println("Successfully created user: " +
                response.getUser().getUserName());    }
}
