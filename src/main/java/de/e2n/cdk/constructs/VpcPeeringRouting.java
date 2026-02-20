package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.ec2.CfnRoute;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates routes in each given route table to each given destination cidr block.
 * Resources:
 * <a href="https://docs.aws.amazon.com/vpc/latest/peering/peering-configurations-full-access.html">AWS - Peering with full access</a>
 * <a href="https://docs.aws.amazon.com/vpc/latest/peering/peering-configurations-partial-access.html"> AWS - Peering with partial access</a>
 */
public class VpcPeeringRouting extends Construct {

    private final List<CfnRoute> routes = new ArrayList<>();

    public VpcPeeringRouting(Construct scope, String id, Props props) {
        super(scope, id);

        int i = 0;
        for (var routeTableId : props.routeTableIds) {
            for (var destinationCidrBlock : props.destinationCidrBlocks) {
                CfnRoute route = CfnRoute.Builder.create(this, props.routeName + "-" + (i++) + "-VpcPeeringConnectionRoute")
                        .routeTableId(routeTableId)
                        .destinationCidrBlock(destinationCidrBlock)
                        .vpcPeeringConnectionId(props.vpcPeeringConnectionId)
                        .build();
                routes.add(route);
            }
        }
    }

    public List<CfnRoute> getRoutes() {
        return routes;
    }

    public static class Props {
        String routeName;
        String vpcPeeringConnectionId;
        Set<String> routeTableIds;
        Set<String> destinationCidrBlocks;

        public Props(String routeName,
                     String vpcPeeringConnectionId,
                     Set<String> routeTableIds,
                     Set<String> destinationCidrBlocks) {
            this.routeName = routeName;
            this.vpcPeeringConnectionId = vpcPeeringConnectionId;
            this.routeTableIds = routeTableIds;
            this.destinationCidrBlocks = destinationCidrBlocks;
        }

    }

    public static Set<String> getRouteTableIds(List<ISubnet> subnets) {
        return subnets.stream()
                .map(subnet -> subnet.getRouteTable().getRouteTableId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
