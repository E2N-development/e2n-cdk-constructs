package de.e2n.cdk.stacks;

import de.e2n.cdk.constructs.VpcPeeringAccepter;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * Creates the IAM Role(s) for accepting a vpc peering connection in the target (main) vpc for a landing zone
 */
public class VpcPeeringAccepterStack extends Stack {

    private final VpcPeeringAccepter vpcPeeringAccepter;

    public VpcPeeringAccepterStack(Construct scope,
                                   String id,
                                   StackProps props,
                                   VpcPeeringAccepter.Props vpcPeeringAccepterProps) {
        super(scope, id, props);
        this.vpcPeeringAccepter = new VpcPeeringAccepter(this, "VpcPeeringAccepter", vpcPeeringAccepterProps);
    }

    public VpcPeeringAccepter getVpcPeeringAccepter() {
        return vpcPeeringAccepter;
    }

}
