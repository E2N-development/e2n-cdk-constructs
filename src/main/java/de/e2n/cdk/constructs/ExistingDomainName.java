package de.e2n.cdk.constructs;

import software.constructs.Construct;
import software.amazon.awscdk.services.apigateway.DomainName;
import software.amazon.awscdk.services.apigateway.DomainNameAttributes;
import software.amazon.awscdk.services.apigateway.IDomainName;

/**
 * Ermöglicht das Referenzieren einer existierenden APIGateway Domain Name Ressource
 * mittels AWS CDK.
 */
public class ExistingDomainName extends Construct {

    private final IDomainName iDomainName;

    /**
     * Erzeugt mittels AWS CDK ein neues Construct, welches einen bestehenden
     * APIGateway Domain Name darstellt und referenziert.
     * @param scope Parent von diesem Construct, normalerweise eine `App` oder `Stage`,
     *              (immer vom Typ `Construct`)
     * @param id Die construct Id von diesem Construct.
     * @param domainName Der Domain Name (z.B. `myapi.de`), der in APIGateway erzeugt werden soll
     * @param aliasHostedZoneId Die Hosted zone ID der Domain Name Ressource
     * @param aliasTarget Der API Gateway Domain Name
     *                    (z.B. `abc1234.execute-api.eu-central-1.amazonaws.com`)
     */
    public ExistingDomainName(final Construct scope,
                              final String id,
                              String domainName,
                              String aliasHostedZoneId,
                              String aliasTarget) {
        super(scope, id);

        this.iDomainName = DomainName.fromDomainNameAttributes(this, id, DomainNameAttributes.builder()
                .domainName(domainName)
                .domainNameAliasHostedZoneId(aliasHostedZoneId)
                .domainNameAliasTarget(aliasTarget)
                .build());
    }

    public IDomainName getiDomainName() {
        return iDomainName;
    }

}
