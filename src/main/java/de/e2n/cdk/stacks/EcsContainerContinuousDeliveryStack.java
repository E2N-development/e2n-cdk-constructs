package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxArmBuildImage;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.EcrSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.EcsDeployAction;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.constructs.Construct;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * The {@code CICDStack} creates a CodePipeline. Every commit to ECR deploys to ECS.
 */
public class EcsContainerContinuousDeliveryStack extends Stack {

    private final String name;
    private final String imageTag;

    public EcsContainerContinuousDeliveryStack(Construct scope,
                                               String id,
                                               StackProps props,
                                               String name,
                                               IRepository repository,
                                               FargateService fargateService,
                                               FargateTaskDefinition fargateTaskDefinition,
                                               String imageTag) {
        super(scope, id, props);

        this.name = name;
        this.imageTag = imageTag;

        var sourceStage = sourceStage(repository);
        var buildStage = buildStage(repository, fargateTaskDefinition);
        var deployStage = deployStage(fargateService);
        var stages = List.of(sourceStage, buildStage, deployStage);

        Pipeline.Builder.create(this, id + "Pipeline")
                .pipelineName(name)
                .stages(stages)
                .build();
    }

    /**
     * Every commit to ECR triggers a change and creates a imageDetail.json (which we do not need?)
     */
    StageProps sourceStage(IRepository repository) {
        var sourceAction = EcrSourceAction.Builder.create()
                .actionName("Source")
                .repository(repository)
                .imageTag(imageTag)
                .output(Artifact.artifact("imageDetail"))
                .build();

        return software.amazon.awscdk.services.codepipeline.StageProps.builder()
                .stageName("Source")
                .actions(List.of(sourceAction))
                .build();
    }

    /**
     * We need to map imageDetail.json from ECR to imagedefinitions.json for ECS deployment.
     * This is unfortunate, but needed. There is no shortcut.
     */
    StageProps buildStage(IRepository repository, FargateTaskDefinition fargateTaskDefinition) {
        var buildSpec = BuildSpec.fromObject(
                new LinkedHashMap<>(
                        SortedMap.of("version", "0.2",
                                     "phases", SortedMap.of("build",
                                                            SortedMap.of("commands",
                                                                         List.of("echo \"[{ \\\"name\\\": \\\"$CONTAINER_NAME\\\", \\\"imageUri\\\": \\\"$REPOSITORY_URI\\\" }]\" > imagedefinitions.json",
                                                       "cat imagedefinitions.json"))),
                                     "artifacts", SortedMap.of("files", List.of("imagedefinitions.json"))
                ))
        );

        // By the power of Graviton! [optional]
        var environment = BuildEnvironment.builder()
                .computeType(ComputeType.SMALL)
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_2_0)
                .build();

        var buildProject = PipelineProject.Builder.create(this, name + "-Build")
                .buildSpec(buildSpec)
                .environment(environment)
                .environmentVariables(SortedMap.of(
                        "CONTAINER_NAME", BuildEnvironmentVariable.builder()
                                .value(fargateTaskDefinition.getDefaultContainer().getContainerName())
                                .build(),
                        "REPOSITORY_URI", BuildEnvironmentVariable.builder()
                                .value(repository.getRepositoryUri() + ":" + imageTag)
                                .build()
                ))
                .build();

        var buildAction = CodeBuildAction.Builder.create()
                .actionName("BuildAction")
                .project(buildProject)
                .input(Artifact.artifact("imageDetail"))
                .outputs(List.of(Artifact.artifact("imagedefinitions")))
                .build();

        return software.amazon.awscdk.services.codepipeline.StageProps.builder()
                .stageName("Build")
                .actions(List.of(buildAction))
                .build();
    }

    /**
     * Deploys our new image in ECR to the ECS cluster.
     */
    StageProps deployStage(FargateService fargateService) {
        var deployAction = EcsDeployAction.Builder.create()
                .actionName("Deploy")
                .service(fargateService)
                .input(Artifact.artifact("imagedefinitions"))
                .build();

        return software.amazon.awscdk.services.codepipeline.StageProps.builder()
                .stageName("Deploy")
                .actions(List.of(deployAction))
                .build();
    }


}
