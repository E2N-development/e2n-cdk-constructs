package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codecommit.IRepository;
import software.amazon.awscdk.services.codepipeline.IPipeline;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.IEventBus;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.targets.CodePipeline;
import software.constructs.Construct;

import java.util.List;

public class PipelinesTriggerStack extends Stack {

    public PipelinesTriggerStack(Construct scope,
                                 String id,
                                 StackProps props,
                                 IEventBus eventBus,
                                 IRepository repository,
                                 String sourceBranch,
                                 List<IPipeline> pipelines) {
        super(scope, id, props);

        int i = 0;
        for (IPipeline pipeline : pipelines) {
            Rule.Builder.create(this, "RepositoryStateChangeRule" + i++)
                    .eventBus(eventBus)
                    .eventPattern(EventPattern.builder()
                                          .source(List.of("aws.codecommit"))
                                          .resources(List.of(repository.getRepositoryArn()))
                                          .detailType(List.of("CodeCommit Repository State Change"))
                                          .detail(SortedMap.of("event", List.of("referenceCreated", "referenceUpdated"),
                                                               "referenceType", List.of("branch"),
                                                               "referenceName", List.of(sourceBranch)))
                                          .build())
                    .targets(List.of(new CodePipeline(pipeline)))
                    .build();
        }
    }

}
