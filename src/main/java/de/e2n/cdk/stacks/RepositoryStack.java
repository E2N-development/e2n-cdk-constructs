package de.e2n.cdk.stacks;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryEncryption;
import software.amazon.awscdk.services.ecr.TagStatus;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.List;

public class RepositoryStack extends Stack {

    private final Repository repository;

    public RepositoryStack(Construct scope, String id, StackProps props, String name) {
        this(scope, id, props, name, List.of());
    }

    public RepositoryStack(Construct scope, String id, StackProps props, String name, List<LifecycleRule> lifecycleRules) {
        super(scope, id, props);

        repository = Repository.Builder.create(this, "Repository")
                .repositoryName(name)
                .encryption(RepositoryEncryption.KMS)
                .removalPolicy(RemovalPolicy.DESTROY)
                .lifecycleRules(lifecycleRules)
                .build();
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * Erzeugt eine Liste von LifecycleRules, die Images mit den übergebenen Tags niemals löscht und alle anderen Images löscht,
     * mit Ausnahme der maxAnzahlZuBehaltenderImages neuesten Images.
     * @param zuBehaltendeImageTags Tags der Images, die niemals gelöscht werden sollen
     * @param maxAnzahlZuBehaltenderImages Anzahl der (neuesten X) Images, die behalten werden sollen
     * @return Liste von LifecycleRules
     */
    public static List<LifecycleRule> buildLifecycleRules(List<String> zuBehaltendeImageTags, int maxAnzahlZuBehaltenderImages) {
        List<LifecycleRule> rules = new ArrayList<>();
        int i = 1;
        for (String imageTag : zuBehaltendeImageTags) {
            rules.add(LifecycleRule.builder()
                              .rulePriority(i++)
                              .tagStatus(TagStatus.TAGGED)
                              .tagPrefixList(List.of(imageTag))
                              .maxImageCount(1)
                              .description("Image mit dem Tag " + imageTag + " behalten")
                              .build());
        }

        rules.add(LifecycleRule.builder()
                          .rulePriority(i)
                          .tagStatus(TagStatus.ANY)
                          .maxImageCount(maxAnzahlZuBehaltenderImages)
                          .description("Insgesamt " + maxAnzahlZuBehaltenderImages + " Images behalten, alle älteren Images löschen")
                          .build());

        return rules;
    }

}
