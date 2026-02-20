package de.e2n.cdk.stacks;

import de.e2n.cdk.constructs.SSMParameter;
import de.e2n.cdk.constructs.SSMParameterReader;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.NsRecord;
import software.amazon.awscdk.services.route53.NsRecordProps;
import software.constructs.Construct;

public class NameServerRecordStack extends Stack {

    private final NsRecord nsRecord;

    public NameServerRecordStack(Construct scope,
                                 String id,
                                 StackProps props,
                                 String hostedZoneId,
                                 String subdomain,
                                 SSMParameter nameServerParameter) {
        super(scope, id, props);
        var nameServerParameterReader = new SSMParameterReader(
                this,
                "SSMParameterReader",
                nameServerParameter);
        var hostedZone = HostedZone.fromHostedZoneId(this, "HostedZone", hostedZoneId);
        var nameServers = Fn.split("\n", nameServerParameterReader.getParameterValue());
        nsRecord = new NsRecord(this, "NsRecord", NsRecordProps.builder()
                .zone(hostedZone)
                .comment("created with CDK in " + getStackId())
                .recordName(subdomain)
                .values(nameServers)
                .build());
    }

    public NsRecord getNsRecord() {
        return nsRecord;
    }

}
