package de.e2n.cdk.constructs;

import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.customresources.SdkCallsPolicyOptions;
import software.constructs.Construct;

import java.time.Instant;
import java.util.HashMap;

/**
 * Ermöglicht das Lesen von SSM-Parameters zur CDK-Laufzeit, z.B. zum Abrufen von CDK/CF erzeugten
 * Queue-ARNs ohne Einsatz von StackDependencies/CF-ImportValue-Konstrukte. Vermeidet harte Deps
 * zwischen Stacks.
 * https://stackoverflow.com/a/59774628
 * Cross-Stack-References innerhalb einer Region funktionieren auch mit Fn.ImportValue
 * oder StackProps innerhalb einer App. Hat aber seine Tücken, deswegen per SSM.
 * Hintergrund: https://stackoverflow.com/a/64849790
 * Neu: Mit AWS RAM ist es möglich, SSM Parameter Cross-Account zu teilen ohne AWS STS (Cross-Account Reader Role)
 */
public class SSMParameterReader extends Construct {

    private final AwsCustomResource customResource;

    public static SSMParameterReader sharedParameter(Construct scope, SharedSSMParameter ssmParameter) {
        var id = ssmParameter.getOwnerAccountId() + ssmParameter.getRegion() + "SSMParameter" + ssmParameter.getParameterName();
        var parameterArn = "arn:aws:ssm:" + ssmParameter.getRegion() + ":" + ssmParameter.getOwnerAccountId() + ":parameter" + ssmParameter.getParameterName();
        return new SSMParameterReader(scope, id, parameterArn, ssmParameter.getRegion());
    }
    /**
     * Liest einen durch AWS RAM in der Organisation geteilten SSM Parameter (Cross-Account).
     * Siehe auch {@link SSMParameterRAMWriter(Construct, String, String, String, String)}
     * https://docs.aws.amazon.com/systems-manager/latest/userguide/parameter-store-shared-parameters.html
     */
    public static SSMParameterReader sharedParameter(Construct scope,
                                                     String id,
                                                     String region,
                                                     String ownerAccountId,
                                                     String parameterName) {
        var parameterArn = "arn:aws:ssm:" + region + ":" + ownerAccountId + ":parameter" + parameterName;
        return new SSMParameterReader(scope, id, parameterArn, region);
    }

    public SSMParameterReader(Construct scope, String id, String parameterName, String region) {
        this(scope, id, parameterName, region, null);
    }

    public SSMParameterReader(Construct scope, String id, SSMParameter ssmParameter) {
        this(scope, id, ssmParameter.getParameterName(), ssmParameter.getRegion(), ssmParameter.getReaderRole().getRoleArn());
    }

    public SSMParameterReader(Construct scope, String id, String parameterName, String region, String assumeRoleArn) {
        super(scope, id);

        var builder = AwsSdkCall.builder()
                .service("SSM")
                .action("getParameter")
                .parameters(new HashMap<>(){{
                    put("Name", parameterName);
                }})
                .region(region)
                .physicalResourceId(PhysicalResourceId.of(String.valueOf(Instant.now().getEpochSecond())));

        if (assumeRoleArn != null) {
            builder.assumedRoleArn(assumeRoleArn);
        }
        var sdkCall = builder.build();

        var policy = AwsCustomResourcePolicy.fromSdkCalls(
                SdkCallsPolicyOptions.builder()
                        .resources(AwsCustomResourcePolicy.ANY_RESOURCE)
                        .build());
        customResource = AwsCustomResource.Builder.create(this, id)
                .onUpdate(sdkCall)
                .policy(policy)
                // https://github.com/aws/aws-cdk/issues/30067
                .installLatestAwsSdk(false)
                .build();
    }

    public String getParameterValue() {
        return customResource.getResponseField("Parameter.Value");
    }

}
