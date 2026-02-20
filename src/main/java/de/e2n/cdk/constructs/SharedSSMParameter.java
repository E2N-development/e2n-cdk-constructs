package de.e2n.cdk.constructs;

public class SharedSSMParameter {

        private final String parameterName;
        private final String ownerAccountId;
        private final String region;

    public SharedSSMParameter(String parameterName, String ownerAccountId, String region) {
        this.parameterName = parameterName;
        this.ownerAccountId = ownerAccountId;
        this.region = region;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getOwnerAccountId() {
        return ownerAccountId;
    }

    public String getRegion() {
        return region;
    }

}
