package de.e2n.cdk.stacks;

import de.e2n.cdk.constructs.VpcPeeringRouting;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public class VpcPeeringRoutingStack extends Stack {

    private final VpcPeeringRouting vpcPeeringRouting;

    public VpcPeeringRoutingStack(
            Construct scope,
            String id,
            StackProps props,
            VpcPeeringRouting.Props vpcPeeringRoutingProps) {
        super(scope, id, props);

        vpcPeeringRouting = new VpcPeeringRouting(this, "VpcPeeringRouting", vpcPeeringRoutingProps);
    }

    public VpcPeeringRouting getVpcPeeringRouting() {
        return vpcPeeringRouting;
    }

}
