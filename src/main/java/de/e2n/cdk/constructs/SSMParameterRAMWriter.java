package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.ram.CfnResourceShare;
import software.amazon.awscdk.services.ssm.ParameterTier;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import java.util.List;

public class SSMParameterRAMWriter extends Construct {

    /**
     * Erstellt einen durch AWS RAM in der Organization geteilten SSM Parameter. Es ist keine Cross-Account Reader Role
     * vonnöten.
     * https://docs.aws.amazon.com/systems-manager/latest/userguide/parameter-store-shared-parameters.html
     */
    public SSMParameterRAMWriter(Construct scope,
                                 String id,
                                 String parameterName,
                                 String parameterValue,
                                 String resourceSharePrincipal) {
        super(scope, id);

        // its not possible to share refs or resources cross stack + cross account. We do it with SSM and a custom resource.
        var ssmParameter = StringParameter.Builder.create(this, "SsmParameter")
                .parameterName(parameterName)
                .tier(ParameterTier.ADVANCED)
                .description("Ssm Parameter created with CDK in " + id).stringValue(parameterValue)
                .build();

        CfnResourceShare cfnResourceShare = CfnResourceShare.Builder.create(this, "ResourceShare")
                // RAM akzeptiert keine Slashes (üblich und valide in SSM Parameternamen),
                // wir ersetzen sie durch Bindestriche
                .name(parameterName.replace("/", "-"))
                .allowExternalPrincipals(false)
                .principals(List.of(resourceSharePrincipal))
                .resourceArns(List.of(ssmParameter.getParameterArn()))
                .build();
    }
}
