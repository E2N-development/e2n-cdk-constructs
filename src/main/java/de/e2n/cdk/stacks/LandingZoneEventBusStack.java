package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.PhysicalName;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.CfnEventBusPolicy;
import software.amazon.awscdk.services.events.CfnRule;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.CloudWatchLogGroup;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.List;

public class LandingZoneEventBusStack extends Stack {

    EventBus eventBus;

    public LandingZoneEventBusStack(Construct scope,
                                    String id,
                                    StackProps props,
                                    String crossAccountAccessAccountId) {
        super(scope, id, props);

        this.eventBus = EventBus.Builder.create(this, "EventBus")
                .eventBusName(PhysicalName.GENERATE_IF_NEEDED)
                .build();

        // Allows cross account access to put events in this bus.
        CfnEventBusPolicy.Builder.create(this, "CrossAccountPolicy")
                .eventBusName(eventBus.getEventBusName())
                .action("events:PutEvents")
                .principal(crossAccountAccessAccountId)
                .statementId("AcceptFrom" + crossAccountAccessAccountId)
                .build();

        var eventBusLogGroup = LogGroup.Builder.create(this, "EventBusLogGroup").build();
        var logRule = Rule.Builder.create(this, "LogRule")
                .eventBus(eventBus)
                .eventPattern(EventPattern.builder()
                                      .source(List.of())
                                      .build())
                .targets(List.of(CloudWatchLogGroup.Builder.create(eventBusLogGroup).build()))
                .build();
        // Workaround https://github.com/aws/aws-cdk/issues/20486
        ((CfnRule) logRule.getNode().getDefaultChild()).setEventPattern(SortedMap.of("source", List.of(SortedMap.of("exists", true))));
    }

    public EventBus getEventBus() {
        return eventBus;
    }

}
