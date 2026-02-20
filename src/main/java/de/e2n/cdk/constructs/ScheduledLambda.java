package de.e2n.cdk.constructs;

import de.e2n.cdk.model.ILambda;
import software.constructs.Construct;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;

import java.util.Arrays;
import java.util.List;

/**
 * Diese Klasse erzeugt mittels AWS CDK eine Schedule getriggerte Lambda.
 *
 * Das CDK-Konstrukt besteht aus den folgenden AWS Ressourcen:
 * - eine AWS Lambda Function inklusive IAM Role, Version & Alias
 * - eine EventBridge Schedule-Rule
 */
public class ScheduledLambda extends Construct {

    private final ILambda lambda;

    public ScheduledLambda(final Construct scope,
                           final String id,
                           ILambda lambda,
                           Schedule schedule) {
        this(scope, id, lambda, List.of(schedule));
    }

    public ScheduledLambda(final Construct scope,
                           final String id,
                           ILambda lambda,
                           List<Schedule> schedules) {
        super(scope, id);

        this.lambda = lambda;

        for (int i = 0; i < schedules.size(); i++) {
            String ruleId = "Rule" + i;
            Rule rule = Rule.Builder.create(this, ruleId)
                    .schedule(schedules.get(i))
                    .targets(Arrays.asList(new LambdaFunction(lambda.getAlias())))
                    .build();
        }
    }

    public ILambda getLambda() {
        return lambda;
    }

}
