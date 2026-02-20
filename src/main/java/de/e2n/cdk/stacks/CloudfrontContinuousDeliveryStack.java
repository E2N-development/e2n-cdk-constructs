package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.IDistribution;
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
import software.amazon.awscdk.services.codepipeline.actions.ManualApprovalAction;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

import java.util.List;

/**
 * The {@code FrontendCICDStack} creates a CodePipeline. Every commit to ECR deploys to S3/Cloudfront.
 */
public class CloudfrontContinuousDeliveryStack extends Stack {

    private final String name;
    private final String imageTag;

    public CloudfrontContinuousDeliveryStack(Construct scope,
                                             String id,
                                             StackProps props,
                                             String name,
                                             IRepository repository,
                                             IBucket originBucket,
                                             IDistribution distribution,
                                             String imageTag) {

        super(scope, id, props);

        this.name = name;
        this.imageTag = imageTag;

        var sourceStage = sourceStage(repository);
        var approvalStage = approvalStage();
        var deployStage = deployStage(repository, originBucket, distribution);
        var stages = List.of(sourceStage, approvalStage, deployStage);

        Pipeline.Builder.create(this, "Pipeline")
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

//     We need here a appoval stage because the frontend pipeline is faster than the backend pipeline and we need to wait.
     StageProps approvalStage() {
        var approvalAction = ManualApprovalAction.Builder.create()
                .actionName("ApprovalAction")
                .build();
        return StageProps.builder()
                .stageName("Approval")
                .actions(List.of(approvalAction))
                .build();
    }

    /**
     * Deploys our new image in ECR to the ECS cluster.
     */
    StageProps deployStage(IRepository repository, IBucket originBucket, IDistribution distribution) {
        var environment = BuildEnvironment.builder()
                .computeType(ComputeType.SMALL)
                .buildImage(LinuxArmBuildImage.AMAZON_LINUX_2_STANDARD_2_0)
                .privileged(true)
                .build();

        // Create the build project that will copy the application to s3 and invalidate the cloudfront cache
        PipelineProject project = PipelineProject.Builder.create(this, name + "-Deploy")
                .environment(environment)
                .environmentVariables(SortedMap.of(
                        "CONTAINER_NAME", BuildEnvironmentVariable.builder()
                                .value(repository.getRepositoryName())
                                .build(),
                        "REPOSITORY_URI", BuildEnvironmentVariable.builder()
                                .value(repository.getRepositoryUri() + ":" + imageTag)
                                .build(),
                        "ORIGIN_BUCKET_S3_URL", BuildEnvironmentVariable.builder()
                                .value(originBucket.s3UrlForObject())
                                .build(),
                        "CLOUDFRONT_ID", BuildEnvironmentVariable.builder()
                                .value(distribution.getDistributionId())
                                .build()
                ))
                .buildSpec(BuildSpec.fromObject(SortedMap.of(
                        "version", "0.2",
                        "phases", SortedMap.of(
                                "install", SortedMap.of(
                                        "runtime-versions", SortedMap.of(
                                                "golang", "latest"),
                                        "commands", List.of(
                                                //"go get -u github.com/awslabs/amazon-ecr-credential-helper/ecr-login/cli/docker-credential-ecr-login",
                                                // https://docs.aws.amazon.com/codebuild/latest/userguide/build-env-ref-env-vars.html
                                                "AWS_ACCOUNT_ID=$(echo $CODEBUILD_BUILD_ARN | cut -d':' -f5)",
                                                "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com")),
                        "build", SortedMap.of(
                                        "commands", List.of(
                                                // extract the static files of the application from the docker image
                                                "docker cp $(docker create --name $CONTAINER_NAME $REPOSITORY_URI):/usr/share/nginx/html . && docker rm $CONTAINER_NAME",
                                                "aws s3 cp html $ORIGIN_BUCKET_S3_URL --exclude index.html --recursive --metadata-directive REPLACE --expires $(date '+%Y-%m-%dT%H:%M:%SZ' -d '+30 days') --cache-control max-age=2592000",
                                                "aws s3 cp html/index.html $ORIGIN_BUCKET_S3_URL --metadata-directive REPLACE --cache-control 'no-cache'",
                                                "aws cloudfront create-invalidation --distribution-id ${CLOUDFRONT_ID} --paths \"/*\""))))))
                .build();

        repository.grantPull(project);
        distribution.grantCreateInvalidation(project);
        originBucket.grantReadWrite(project);

        var deployAction = CodeBuildAction.Builder.create()
                .actionName("DeployToCloudfront")
                .project(project)
                .input(Artifact.artifact("imageDetail"))
                .build();

        return software.amazon.awscdk.services.codepipeline.StageProps.builder()
                .stageName("Deploy")
                .actions(List.of(deployAction))
                .build();
    }

}
