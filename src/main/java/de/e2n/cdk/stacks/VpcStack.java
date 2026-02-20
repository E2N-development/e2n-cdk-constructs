package de.e2n.cdk.stacks;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.IpAddresses;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

public class VpcStack extends Stack {

    private final Vpc vpc;

    public VpcStack(Construct scope, String id, StackProps props, String cidr) {
        this(scope, id, props, cidr, null);
    }

    public VpcStack(Construct scope, String id, StackProps props, String cidr, Integer natGateways) {
        super(scope, id, props);

        var builder = Vpc.Builder.create(this, "Vpc")
                .ipAddresses(IpAddresses.cidr(cidr));

        if (natGateways != null) {
            builder.natGateways(natGateways);
        }

        this.vpc = builder.build();
    }

    public Vpc getVpc() {
        return vpc;
    }

}
