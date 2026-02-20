package de.e2n.cdk.constructs;

import software.amazon.awscdk.CfnTag;
import software.amazon.awscdk.services.ec2.CfnVPCPeeringConnection;
import software.constructs.Construct;

import java.util.List;

/**
 * Creates a vpc peering connection from given source (requester) environment towards the declared target (accepting) env.
 * #{@link VpcPeeringAccepter} must be created first in the target (accepter) stack
 * Resources:
 * <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/peer-with-vpc-in-another-account.html">Walkthrough: Peer with an VPC in another AWS account</a>
 */
public class VpcPeeringRequester extends Construct {

    private final CfnVPCPeeringConnection vpcPeeringConnection;

    public VpcPeeringRequester(Construct scope, String id, Props props) {
        super(scope, id);

        // https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_ec2.CfnVPCPeeringConnection.html
        this.vpcPeeringConnection = CfnVPCPeeringConnection.Builder.create(this, "VpcPeeringConnection")
                .vpcId(props.requesterVpcId)
                .peerVpcId(props.accepterVpcId)
                .peerOwnerId(props.accepterAwsAccountId)
                .peerRegion(props.accepterRegion)
                .peerRoleArn(props.accepterRoleArn)
                .tags(List.of(CfnTag.builder()
                                      .key("landing-zone")
                                      .value(props.landingZoneName)
                                      .build()))
                .build();
    }

    public CfnVPCPeeringConnection getVpcPeeringConnection() {
        return vpcPeeringConnection;
    }

    public static class Props {
        // the name of the requester
        String landingZoneName;
        // source vpc of the peering connection (requester)
        String requesterVpcId;

        // target vpc of the peering connection (accepter)
        String accepterVpcId;
        String accepterAwsAccountId;
        String accepterRegion;
        String accepterRoleArn;

        public Props(String landingZoneName,
                     String requesterVpcId,
                     String accepterVpcId,
                     String accepterAwsAccountId,
                     String accepterRegion,
                     String accepterRoleArn) {
            this.landingZoneName = landingZoneName;
            this.requesterVpcId = requesterVpcId;
            this.accepterVpcId = accepterVpcId;
            this.accepterAwsAccountId = accepterAwsAccountId;
            this.accepterRegion = accepterRegion;
            this.accepterRoleArn = accepterRoleArn;
        }

    }

}


