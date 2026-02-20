package de.e2n.cdk.stacks;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.kms.Key;
import software.constructs.Construct;

public class KeyStack extends Stack {

    private final Key key;

    public KeyStack(Construct scope, String id, StackProps props, String keyAlias, String description) {
        super(scope, id, props);

        this.key = Key.Builder.create(this, "Key")
                .description(description)
                .alias(keyAlias)
                .build();
    }

    public Key getKey() {
        return key;
    }

}
