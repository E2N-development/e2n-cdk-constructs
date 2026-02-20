package de.e2n.cdk.constructs;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.PhysicalName;
import software.amazon.awscdk.services.iam.AccountPrincipal;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

import java.util.List;

/**
 * Creates a IAM role dedicated to accepting a vpc peering connection request from given requester.
 * This Stack must only be created once within each target (accepting) stack.
 * Resources:
 * <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/peer-with-vpc-in-another-account.html">Walkthrough: Peer with an VPC in another AWS account</a>
 */
public class VpcPeeringAccepter extends Construct {

    /** Needs to be referenced in {@link VpcPeeringRequester}
     * as {@link software.amazon.awscdk.services.ec2.CfnVPCPeeringConnection.Builder#peerRoleArn(String)}
     */
    private final Role accepterRole;

    public VpcPeeringAccepter(Construct scope, String id, Props props) {
        super(scope, id);

        var accepterPolicy = PolicyDocument.Builder
                .create()
                .statements(List.of(
                        PolicyStatement.Builder.create()
                                .actions(List.of("ec2:AcceptVpcPeeringConnection"))
                                .resources(List.of("arn:aws:ec2:" + props.accepterRegion + ":" + props.accepterAwsAccountId + ":vpc/" + props.accepterVpcId))
                                .build(),
                        PolicyStatement.Builder.create()
                                .actions(List.of("ec2:AcceptVpcPeeringConnection"))
                                .conditions(SortedMap.of("StringEquals", SortedMap.of("ec2:AccepterVpc", "arn:aws:ec2:" + props.accepterRegion + ":" + props.accepterAwsAccountId + ":vpc/" + props.accepterVpcId)))
                                .resources(List.of("arn:aws:ec2:" + props.accepterRegion + ":" + props. accepterAwsAccountId + ":vpc-peering-connection/*"))
                                .build()))
                .build();
        accepterRole = Role.Builder.create(this, "VpcPeeringConnectionAccepterRole")
                //  in a cross-environment fashion, the resource's physical name must be explicit set or use `PhysicalName.GENERATE_IF_NEEDED`
                // https://stackoverflow.com/a/63165557
                .roleName(PhysicalName.GENERATE_IF_NEEDED)
                .assumedBy(new AccountPrincipal(props.requesterAwsAccountId))
                .inlinePolicies(SortedMap.of("VpcPeeringConnectionAccepterPolicy", accepterPolicy))
                .build();
    }

    public Role getAccepterRole() {
        return accepterRole;
    }

    public static class Props {
        // source vpc of the peering connection (requester)
        String requesterAwsAccountId;

        // target vpc of the peering connection (accepter)
        String accepterVpcId;
        String accepterAwsAccountId;
        String accepterRegion;

        public Props(String requesterAwsAccountId,
                     String accepterVpcId,
                     String accepterAwsAccountId,
                     String accepterRegion) {
            super();
            this.requesterAwsAccountId = requesterAwsAccountId;
            this.accepterVpcId = accepterVpcId;
            this.accepterAwsAccountId = accepterAwsAccountId;
            this.accepterRegion = accepterRegion;
        }

    }

}
