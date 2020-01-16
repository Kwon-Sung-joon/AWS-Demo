package kr.co.starlabs.cloudAWS;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateResult;

/**
 * Gets a server certificate
 */
public class GetServerCertificateTests {

    public static void main(String[] args) {

        final String USAGE =
            "To run this example, supply a certificate name\n" +
            "Ex: GetServerCertificate <certificate-name>\n";

        if (args.length != 1) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String cert_name = args[0];

        final AmazonIdentityManagement iam =
            AmazonIdentityManagementClientBuilder.defaultClient();

        GetServerCertificateRequest request = new GetServerCertificateRequest()
                    .withServerCertificateName(cert_name);

        GetServerCertificateResult response = iam.getServerCertificate(request);

        System.out.format("Successfully retrieved certificate with body %s",
                response.getServerCertificate().getCertificateBody());
    }
}