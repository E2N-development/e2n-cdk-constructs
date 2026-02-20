package de.e2n.cdk.model;

import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Version;

public interface ILambda {

    Function getFunction();
    Version getVersion();
    Alias getAlias();

}
