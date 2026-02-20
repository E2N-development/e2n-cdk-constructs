package de.e2n.cdk.stacks;

import de.e2n.cdk.utils.SortedMap;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class GithubActionRoleStack extends Stack {

    private static final String CLIENT_ID = "sts.amazonaws.com";

    // The format of githubRepositoryCondition defines for which GitHub Organization, Repository and Branch the role is valid.
    // see https://aws.amazon.com/de/blogs/security/use-iam-roles-to-connect-github-actions-to-actions-in-aws/
    public GithubActionRoleStack(Construct scope, String id, StackProps props, String githubRepositoryCondition) {
        super(scope, id, props);

        var oidcProvider = OpenIdConnectProvider.Builder.create(this, "token.actions.githubusercontent.com")
                .url("https://token.actions.githubusercontent.com")
                .clientIds(List.of(CLIENT_ID))
                .build();

        var githubActionsECRBasePolicy = PolicyDocument.Builder.create()
                .statements(List.of(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of(
                                "ecr:GetAuthorizationToken",
                                "ecr:GetDownloadUrlForLayer",
                                "ecr:ListImages",
                                "ecr:BatchGetImage",
                                "ecr:DescribeImages",
                                "ecr:InitiateLayerUpload",
                                "ecr:UploadLayerPart",
                                "ecr:CompleteLayerUpload",
                                "ecr:BatchCheckLayerAvailability",
                                "ecr:DescribeRepositories",
                                "ecr:PutImage"))
                        .resources(List.of("*"))
                        .build()))
                .build();

        var githubActionRole = Role.Builder.create(this, "GitHubActions")
                .roleName("GitHubActions")
                .inlinePolicies(SortedMap.of("GithubActionsECRBasePolicy", githubActionsECRBasePolicy))
                .assumedBy(new OpenIdConnectPrincipal(oidcProvider)
                        .withConditions(Map.of(
                                "StringEquals",
                                Map.of("token.actions.githubusercontent.com:aud", CLIENT_ID),
                                "StringLike",
                                Map.of("token.actions.githubusercontent.com:sub", githubRepositoryCondition))))
                .build();
    }
}
