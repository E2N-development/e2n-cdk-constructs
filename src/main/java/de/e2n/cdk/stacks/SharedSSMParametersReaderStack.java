package de.e2n.cdk.stacks;

import de.e2n.cdk.constructs.SSMParameterReader;
import de.e2n.cdk.constructs.SharedSSMParameter;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SharedSSMParametersReaderStack extends Stack {

    private final Map<SharedSSMParameter, String> parameterValues;

    public SharedSSMParametersReaderStack(Construct scope, String id, StackProps props, SharedSSMParameter ssmParameter) {
        this(scope, id, props, List.of(ssmParameter));
    }

    public SharedSSMParametersReaderStack(Construct scope, String id, StackProps props, List<SharedSSMParameter> ssmParameters) {
        super(scope, id, props);

        this.parameterValues = ssmParameters.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        param ->
                                SSMParameterReader.sharedParameter(
                                                this,
                                                param.getOwnerAccountId() + param.getRegion() + "SSMParameter" + param.getParameterName(),
                                                param.getRegion(),
                                                param.getOwnerAccountId(),
                                                param.getParameterName())
                                        .getParameterValue()));
    }

    public Map<SharedSSMParameter,String> getParameterValues() {
        return parameterValues;
    }

}
