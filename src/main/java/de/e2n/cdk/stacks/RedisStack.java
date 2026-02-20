package de.e2n.cdk.stacks;

import software.amazon.awscdk.Fn;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Connections;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.elasticache.CfnParameterGroup;
import software.amazon.awscdk.services.elasticache.CfnSubnetGroup;
import software.amazon.awscdk.services.kms.IKey;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisStack extends Stack {

    private static final int REDIS_DEFAULT_PORT = 6379;

    private final CfnCacheCluster cacheCluster;
    private final SecurityGroup clusterSecurityGroup;
    private final Connections connections;

    public RedisStack(Construct scope,
                      String id,
                      StackProps props,
                      IVpc vpc,
                      String cacheNodeType,
                      IKey secretsManagerKey) {
        super(scope, id, props);

        var subnetIds = vpc.getPrivateSubnets().stream()
                .map(ISubnet::getSubnetId)
                .collect(Collectors.toList());
        var cacheSubnetGroup = CfnSubnetGroup.Builder.create(this, "SubnetGroup")
                .description("RedisStack SubnetGroup")
                .subnetIds(subnetIds)
                .build();

        this.clusterSecurityGroup = SecurityGroup.Builder.create(this, "SecurityGroup")
                .description("RedisCluster security group")
                .vpc(vpc)
                .build();

        this.connections = Connections.Builder.create()
                .defaultPort(Port.tcp(REDIS_DEFAULT_PORT))
                .securityGroups(List.of(clusterSecurityGroup))
                .build();

        CfnParameterGroup cfnParameterGroup = CfnParameterGroup.Builder
                .create(this, "CfnParameterGroup")
                .cacheParameterGroupFamily("redis7")
                .description("ParameterGroup - per CDK erzeugt")
                .build();
        this.cacheCluster = CfnCacheCluster.Builder.create(this, "CacheCluster")
                .vpcSecurityGroupIds(List.of(clusterSecurityGroup.getSecurityGroupId()))
                .engine("redis")
                .cacheSubnetGroupName(Fn.ref(cacheSubnetGroup.getLogicalId()))
                .cacheParameterGroupName(Fn.ref(cfnParameterGroup.getLogicalId()))
                .numCacheNodes(1)
                .cacheNodeType(cacheNodeType)
                .autoMinorVersionUpgrade(true)
                .networkType("ipv4")
                .snapshotRetentionLimit(7)
                .build();

        var cacheClusterSecret = new Secret(
                this,
                "CacheClusterSecret",
                SecretProps.builder()
                        .secretName("RedisClusterSecret")
                        .description("RedisCluster relevante Secrets")
                        .encryptionKey(secretsManagerKey)
                        .secretObjectValue(
                                Map.of("URL", SecretValue.unsafePlainText(cacheCluster.getAttrRedisEndpointAddress())))
                        .build());
    }

    public CfnCacheCluster getCacheCluster() {
        return cacheCluster;
    }

    public SecurityGroup getClusterSecurityGroup() {
        return clusterSecurityGroup;
    }

    public Connections getConnections() {
        return connections;
    }

}
