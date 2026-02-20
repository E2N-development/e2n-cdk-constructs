package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.Cache;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.LinuxArmBuildImage;
import software.amazon.awscdk.services.codebuild.LocalCacheMode;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeCommitTrigger;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.constructs.Construct;

import java.util.List;

public class QuarkusBackendContinuousIntegrationStack extends Stack {

    private final Pipeline pipeline;
    private final String name;
    private final String sourceBranch;
    private final String applicationArtifactId;

    public QuarkusBackendContinuousIntegrationStack(Construct scope,
                                                    String id,
                                                    StackProps props,
                                                    String name,
                                                    String sourceBranch,
                                                    String applicationArtifactId,
                                                    software.amazon.awscdk.services.codecommit.IRepository gitRepo,
                                                    IRepository ecrRepository) {
        super(scope, id, props);

        this.name = name;
        this.sourceBranch = sourceBranch;
        this.applicationArtifactId = applicationArtifactId;

        var sourceStage = sourceStage(gitRepo);
        var buildStage = buildStage(ecrRepository);
        var stages = List.of(sourceStage, buildStage);

        this.pipeline = Pipeline.Builder.create(this, "Pipeline")
                .pipelineName(name)
                .stages(stages)
                .build();
    }

    StageProps sourceStage(software.amazon.awscdk.services.codecommit.IRepository repository) {
        var sourceAction = CodeCommitSourceAction.Builder.create()
                .actionName("Source")
                .repository(repository)
                .branch(sourceBranch)
                .output(Artifact.artifact("sourceArtifact"))
                .trigger(CodeCommitTrigger.NONE)
                .build();

        return StageProps.builder()
                .stageName("Source")
                .actions(List.of(sourceAction))
                .build();
    }

    StageProps buildStage(IRepository ecrRepo) {
        var environment = BuildEnvironment.builder()
                .computeType(ComputeType.MEDIUM)
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_3_0)
                .privileged(true)
                .build();

        var buildProject = PipelineProject.Builder.create(this, name)
                .projectName(name + "-Build")
                // https://docs.aws.amazon.com/codebuild/latest/userguide/build-spec-ref.html#build-spec-ref-example
                .buildSpec(BuildSpec.fromObject(
                        SortedMap.of("version", "0.2",
                                     "phases", SortedMap.of(
                                        "install", SortedMap.of(
                                                "runtime-versions", SortedMap.of(
                                                        "java", "corretto21"),
                                                "commands", List.of(
                                                        "java --version",
                                                        "mvn -v",
                                                        "docker -v",
                                                        // https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html
                                                        "AWS_ACCOUNT_ID=$(echo $CODEBUILD_BUILD_ARN | cut -d':' -f5)",
                                                        "AWS_ECR_REGISTRY=\"$AWS_ACCOUNT_ID.dkr.ecr.eu-central-1.amazonaws.com\"",
                                                        // ecr login
                                                        "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ECR_REGISTRY",
                                                        "export COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-8)",
                                                        "export TESTCONTAINERS_CHECKS_DISABLE=true",
                                                        "export TESTCONTAINERS_REUSE_ENABLE=true",
                                                        "export TESTCONTAINERS_RYUK_DISABLED=true",
                                                        // to avoid docker hub rate limiting we always use our own registry
                                                        // https://www.testcontainers.org/features/image_name_substitution/#automatically-modifying-docker-hub-image-names
                                                        "export TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=$AWS_ECR_REGISTRY",
                                                        "export TESTCONTAINERS_TINYIMAGE_CONTAINER_IMAGE=public.ecr.aws/docker/library/alpine:3.16"
                                                )
                                        ),
                                        "build", SortedMap.of(
                                                "commands", List.of(
                                                        "export QUARKUS_CONTAINER_IMAGE_BUILD=TRUE",
                                                        "export QUARKUS_CONTAINER_IMAGE_REGISTRY=$(echo $REPOSITORY_URI | cut -d'/' -f1)",
                                                        "export QUARKUS_CONTAINER_IMAGE_NAME=$REPOSITORY_NAME",
                                                        "export QUARKUS_CONTAINER_IMAGE_TAG=latest",
                                                        "export QUARKUS_CONTAINER_IMAGE_ADDITIONAL_TAGS=$COMMIT_HASH",
                                                        "mvn spotless:check -pl :" + applicationArtifactId + " -am -T1C -B -ntp",
                                                        "mvn install -pl :" + applicationArtifactId + " -am -T1C -B -ntp",
                                                        "docker images",
                                                        "docker push $REPOSITORY_URI:$QUARKUS_CONTAINER_IMAGE_TAG",
                                                        "docker push $REPOSITORY_URI:$QUARKUS_CONTAINER_IMAGE_ADDITIONAL_TAGS"
                                                )
                                        )
                                )
                        ))
                )
                .environment(environment)
                .environmentVariables(SortedMap.of(
                        "REPOSITORY_NAME", BuildEnvironmentVariable.builder()
                                .value(ecrRepo.getRepositoryName())
                                .build(),
                        "REPOSITORY_URI", BuildEnvironmentVariable.builder()
                                .value(ecrRepo.getRepositoryUri())
                                .build()
                ))
                .cache(Cache.local(LocalCacheMode.DOCKER_LAYER, LocalCacheMode.SOURCE))
                .build();

        ecrRepo.grantPullPush(buildProject);
        buildProject.getRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .actions(List.of("ecr:GetDownloadUrlForLayer",
                                         "ecr:BatchGetImage",
                                         "ecr:DescribeImages",
                                         "ecr:DescribeRepositories",
                                         "ecr:ListImages",
                                         "ecr:BatchCheckLayerAvailability",
                                         "ecr:DescribeRegistry",
                                         "ecr:DescribePullThroughCacheRules",
                                         "ecr:GetAuthorizationToken"))
                        .resources(List.of("*"))
                        .build());

        var buildAction = CodeBuildAction.Builder.create()
                .actionName("BuildAction")
                .project(buildProject)
                .input(Artifact.artifact("sourceArtifact"))
                .build();

        return software.amazon.awscdk.services.codepipeline.StageProps.builder()
                .stageName("Build")
                .actions(List.of(buildAction))
                .build();
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

}
