package de.e2n.cdk.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.services.batch.EcsFargateContainerDefinition;
import software.amazon.awscdk.services.batch.EcsJobDefinition;
import software.amazon.awscdk.services.batch.JobQueue;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;

/**
 * Diese Klasse erzeugt mittels AWS CDK ein per Schedule(s) getriggerten AWS Batch Job auf
 * Fargate-Container Basis.
 */
public class BatchJob extends Construct {

    public BatchJob(final Construct scope,
                    final String id,
                    final BatchJobConfig batchJobConfig) {
        super(scope, id);

        var servicePrincipal = ServicePrincipal.Builder
                .create("ecs-tasks.amazonaws.com")
                .build();
        var jobRole = Role.Builder.create(this, "JobRole")
                .inlinePolicies(batchJobConfig.getInlinePolicies())
                .assumedBy(servicePrincipal)
                .build();
        var executionRole = Role.Builder.create(this, "ExecutionRole")
                .managedPolicies(List.of(ManagedPolicy.fromManagedPolicyArn(
                        this,
                        "AmazonECSTaskExecutionRolePolicy",
                        "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy")))
                .assumedBy(servicePrincipal)
                .build();

        var image = DockerImageAsset.Builder
                .create(this, "DockerImageAsset")
                .directory(batchJobConfig.getDockerfilePath())
                .build();

        var jobDefinition = EcsJobDefinition.Builder.create(this, "JobDefinition")
                .retryAttempts(1)   //RetryAttempts must be between 1 and 10
                .timeout(batchJobConfig.getTimeout())
                .container(EcsFargateContainerDefinition.Builder
                                   .create(this, "ECSFargateContainerDefinition")
                                   .command(batchJobConfig.getJobCommand())
                                   .jobRole(jobRole)
                                   .executionRole(executionRole)
                                   .image(ContainerImage.fromDockerImageAsset(image))
                                   .cpu(batchJobConfig.getvCpus())
                                   .memory(Size.mebibytes(batchJobConfig.getMemoryLimitMiB()))
                                   .logging(LogDriver.awsLogs(AwsLogDriverProps.builder().streamPrefix(id).build())).build())
                .build();

        var jobQueue = JobQueue.Builder.create(this, "JobQueue")
                .computeEnvironments(batchJobConfig.getOrderedComputeEnvironments())
                .build();

        var schedules = batchJobConfig.getSchedules();
        for (int i = 0; i < schedules.size(); i++) {
            String ruleId = "Rule" + i;
            Rule rule = Rule.Builder.create(this, ruleId)
                    .schedule(schedules.get(i))
                    .build();
            rule.addTarget(software.amazon.awscdk.services.events.targets.BatchJob.Builder
                                   .create(jobQueue.getJobQueueArn(),
                                           jobQueue,
                                           jobDefinition.getJobDefinitionArn(),
                                           jobDefinition)
                                   .retryAttempts(0)
                                   .maxEventAge(Duration.hours(2))
                                   .build());
        }
    }

}
