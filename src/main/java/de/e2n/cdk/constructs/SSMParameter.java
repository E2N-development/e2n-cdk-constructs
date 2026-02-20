package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.iam.Role;

public class SSMParameter {

    private final String parameterName;
    private final Role readerRole;
    private final String region;

    public SSMParameter(String parameterName, Role readerRole, String region) {
        this.parameterName = parameterName;
        this.readerRole = readerRole;
        this.region = region;
    }

    public String getParameterName() {
        return parameterName;
    }

    public Role getReaderRole() {
        return readerRole;
    }

    public String getRegion() {
        return region;
    }

}
