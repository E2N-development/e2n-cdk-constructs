package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariableType;
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
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

import java.util.List;


public class ReactFrontendContinuousIntegrationStack extends Stack {

    private final Pipeline pipeline;
    private final String name;
    private final String dockerfilePath;

    public ReactFrontendContinuousIntegrationStack(Construct scope,
                                                   String id,
                                                   StackProps props,
                                                   String name,
                                                   String dockerfilePath,
                                                   software.amazon.awscdk.services.codecommit.IRepository gitRepo,
                                                   IRepository ecrRepository,
                                                   ISecret npmAuthToken) {
        super(scope, id, props);

        this.name = name;
        this.dockerfilePath = dockerfilePath;

        var sourceStage = sourceStage(gitRepo);
        var buildStage = buildStage(ecrRepository, npmAuthToken);
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
                .branch("staging")
                .output(Artifact.artifact("sourceArtifact"))
                .trigger(CodeCommitTrigger.NONE)
                .build();

        return StageProps.builder()
                .stageName("Source")
                .actions(List.of(sourceAction))
                .build();
    }

    StageProps buildStage(IRepository ecrRepo, ISecret npmAuthToken) {
        var environment = BuildEnvironment.builder()
                .computeType(ComputeType.SMALL)
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_2_0)
                .privileged(true)
                .build();

        var buildProject = PipelineProject.Builder.create(this, name)
                .projectName(name + "-Build")
                // https://docs.aws.amazon.com/codebuild/latest/userguide/build-spec-ref.html#build-spec-ref-example
                .buildSpec(BuildSpec.fromObject(
                        SortedMap.of("version", "0.2",
                                     "phases", SortedMap.of(
                                        "install", SortedMap.of(
                                                "runtime-versions", SortedMap.of("docker", "latest", "golang", "latest"),
                                                "commands", List.of(
                                                        // ecr login
                                                        //"go get -u github.com/awslabs/amazon-ecr-credential-helper/ecr-login/cli/docker-credential-ecr-login",
                                                        // https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html
                                                        "AWS_ACCOUNT_ID=$(echo $CODEBUILD_BUILD_ARN | cut -d':' -f5)",
                                                        "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com",
                                                        "export COMMIT_HASH=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-8)"
                                                )
                                        ),
                                        "build", SortedMap.of(
                                                "commands", List.of(
                                                        // Set tags
                                                        "export CONTAINER_IMAGE_TAG=staging",
                                                        "export CONTAINER_IMAGE_ADDITIONAL_TAGS=$COMMIT_HASH",
                                                        // build from Dockerfile.prod
                                                        "docker build --build-arg NPM_AUTH_TOKEN=$NPM_AUTH_TOKEN -t $REPOSITORY_URI:$CONTAINER_IMAGE_TAG -f " + dockerfilePath + " .",
                                                        "docker tag $REPOSITORY_URI:$CONTAINER_IMAGE_TAG $REPOSITORY_URI:$CONTAINER_IMAGE_ADDITIONAL_TAGS",
                                                        "docker images",
                                                        // docker push to ecr registry
                                                        "docker push $REPOSITORY_URI:$CONTAINER_IMAGE_TAG",
                                                        "docker push $REPOSITORY_URI:$CONTAINER_IMAGE_ADDITIONAL_TAGS"
                                                )
                                        )
                                )
                        ))
                )
                .environment(environment)
                .environmentVariables(SortedMap.of(
                        "NPM_AUTH_TOKEN", BuildEnvironmentVariable.builder()
                                .type(BuildEnvironmentVariableType.SECRETS_MANAGER)
                                .value(npmAuthToken.getSecretArn())
                                .build(),
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
        npmAuthToken.grantRead(buildProject);

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
