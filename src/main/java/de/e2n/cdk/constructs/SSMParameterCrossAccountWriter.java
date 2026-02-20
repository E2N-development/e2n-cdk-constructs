package de.e2n.cdk.constructs;

import software.amazon.awscdk.PhysicalName;
import software.amazon.awscdk.services.iam.PrincipalBase;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.ssm.ParameterTier;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class SSMParameterCrossAccountWriter extends Construct {

    private final SSMParameter ssmParameter;

    public SSMParameterCrossAccountWriter(Construct scope,
                                          String id,
                                          String parameterName,
                                          String parameterValue,
                                          String region,
                                          PrincipalBase crossAccountSsmReaderRolePrincipal) {
        super(scope, id);

        // its not possible to share refs or resources cross stack + cross account. We do it with SSM and a custom resource.
        var ssmParameter = StringParameter.Builder.create(this, "SsmParameter")
                .parameterName(parameterName)
                .tier(ParameterTier.STANDARD)
                .description("Ssm Parameter created with CDK in " + id)
                .stringValue(parameterValue)
                .build();

        Role crossAccountSsmParameterReaderRole;
        // We need this role for allowing the other account to assume role and read the SSMParameter
        crossAccountSsmParameterReaderRole = Role.Builder.create(this, "ReaderRole")
                .roleName(PhysicalName.GENERATE_IF_NEEDED)
                .assumedBy(crossAccountSsmReaderRolePrincipal)
                .build();
        ssmParameter.grantRead(crossAccountSsmParameterReaderRole);

        this.ssmParameter = new SSMParameter(ssmParameter.getParameterName(),
                                             crossAccountSsmParameterReaderRole,
                                             region);
    }

    public SSMParameter getSsmParameter() {
        return ssmParameter;
    }

}
