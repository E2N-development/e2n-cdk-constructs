package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.iam.PolicyDocument;

/**
 * Eine IAM-Policy besteht aus dem Namen der Policy und einem Dokument, welches die Statements der
 * Policy beinhaltet.
 */
public class Policy {

    private final String name;
    private final PolicyDocument document;

    /**
     * @param name Der Name der Policy
     * @param document Das {@link PolicyDocument} der Policy
     */
    public Policy(String name, PolicyDocument document) {
        this.name = name;
        this.document = document;
    }

    /**
     *
     * @return Der Name der Policy.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Das {@link PolicyDocument} der Policy.
     */
    public PolicyDocument getDocument() {
        return document;
    }

}
