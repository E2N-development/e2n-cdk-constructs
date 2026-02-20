package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import java.util.LinkedHashMap;
import java.util.List;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.sns.ITopic;
import software.constructs.Construct;

public class InspectorFindingsAlarmStack extends Stack {

    public InspectorFindingsAlarmStack(Construct scope, String id, StackProps props, ITopic teamsAlarm) {
        super(scope, id, props);

        Rule inspectorFindingRule = Rule.Builder.create(this, "InspectorFindingRule")
                .eventPattern(EventPattern.builder()
                        .source(List.of("aws.inspector2"))
                        .detailType(List.of("Inspector2 Finding"))
                        .detail(new LinkedHashMap<>(
                                SortedMap.of("severity", List.of("CRITICAL"), "status", List.of("ACTIVE"))))
                        .build())
                .targets(List.of(new software.amazon.awscdk.services.events.targets.SnsTopic(teamsAlarm)))
                .build();
    }
}
