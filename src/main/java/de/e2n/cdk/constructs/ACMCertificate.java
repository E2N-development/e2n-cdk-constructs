package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

public class ACMCertificate extends Construct {

    private final Certificate certificate;

    public ACMCertificate(Construct scope, String id, String domain, IHostedZone hostedZone) {
        super(scope, id);

        certificate = Certificate.Builder.create(this, "Certificate")
                .domainName(domain)
                .validation(CertificateValidation.fromDns(hostedZone))
                .build();
    }

    public Certificate getCertificate() {
        return certificate;
    }

}
