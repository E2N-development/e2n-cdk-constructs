package de.e2n.cdk.stacks;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerInsights;
import software.constructs.Construct;

public class EcsStack extends Stack {

    private final Cluster cluster;

    public EcsStack(Construct scope,
                    String id,
                    StackProps props,
                    Vpc vpc) {
        this(scope, id, props, vpc, ContainerInsights.ENABLED);
    }

    public EcsStack(Construct scope,
                    String id,
                    StackProps props,
                    Vpc vpc,
                    ContainerInsights containerInsights) {
        super(scope, id, props);

        cluster = Cluster.Builder.create(this, "Cluster")
                .vpc(vpc)
                .enableFargateCapacityProviders(true)
                .containerInsightsV2(containerInsights)
                .build();

    }

    public Cluster getCluster() {
        return cluster;
    }

}
