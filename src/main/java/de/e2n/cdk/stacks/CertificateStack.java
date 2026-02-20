package de.e2n.cdk.stacks;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

import java.util.List;

public class CertificateStack extends Stack {

    private final Certificate certificate;

    public CertificateStack(Construct scope,
                            String id,
                            StackProps props,
                            String domain,
                            IHostedZone hostedZone) {
        this(scope, id, props, domain, List.of(), hostedZone);
    }

    public CertificateStack(Construct scope,
                            String id,
                            StackProps props,
                            String domain,
                            List<String> alternativeDomains,
                            IHostedZone hostedZone) {
        super(scope, id, props);

        var certificateBuilder = Certificate.Builder.create(this, "Certificate")
                .domainName(domain)
                .validation(CertificateValidation.fromDns(hostedZone));
        if (!alternativeDomains.isEmpty()) {
            certificateBuilder.subjectAlternativeNames(alternativeDomains);
        }

        certificate = certificateBuilder.build();
    }

    public Certificate getCertificate() {
        return certificate;
    }

}
