package de.e2n.cdk.stacks;

import de.e2n.cdk.constructs.VpcPeeringRequester;
import software.amazon.awscdk.PhysicalName;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.PrincipalBase;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class VpcPeeringRequesterStack extends Stack {

    private final VpcPeeringRequester vpcPeeringRequester;
    private final String vpcPeeringConnectionRefSsmParameterName;
    private final Role crossAccountSsmParameterReaderRole;

    public VpcPeeringRequesterStack(
            Construct scope,
            String id,
            StackProps props,
            VpcPeeringRequester.Props vpcPeeringRequesterProps,
            PrincipalBase crossAccountSsmReaderRolePrincipal) {
        super(scope, id, props);

        vpcPeeringRequester = new VpcPeeringRequester(this, "VpcPeeringRequester", vpcPeeringRequesterProps);
        var vpcPeeringConnection = vpcPeeringRequester.getVpcPeeringConnection();

        // its not possible to share refs or resources cross stack + cross account. We do it with SSM and a custom resource.
        var ssmParameter = StringParameter.Builder.create(this, "VpcPeeringConnectionRefSsmParameter")
                .parameterName("/" + id + "/VpcConnectionRef")
                .description("cloudformation reference of our vpc peering connection between the landing zone and our main account")
                .stringValue(vpcPeeringConnection.getRef())
                .build();
        this.vpcPeeringConnectionRefSsmParameterName = ssmParameter.getParameterName();
        // We need this role for allowing the main account to assume role
        // and read the SSMParameter in the landing zone account
        this.crossAccountSsmParameterReaderRole = Role.Builder.create(this, "ReaderRole")
                .roleName(PhysicalName.GENERATE_IF_NEEDED)
                .assumedBy(crossAccountSsmReaderRolePrincipal)
                .build();
        ssmParameter.grantRead(crossAccountSsmParameterReaderRole);
    }

    public VpcPeeringRequester getVpcPeeringRequester() {
        return vpcPeeringRequester;
    }

    public String getVpcPeeringConnectionRefSsmParameterName() {
        return vpcPeeringConnectionRefSsmParameterName;
    }

    public Role getCrossAccountSsmParameterReaderRole() {
        return crossAccountSsmParameterReaderRole;
    }

}
