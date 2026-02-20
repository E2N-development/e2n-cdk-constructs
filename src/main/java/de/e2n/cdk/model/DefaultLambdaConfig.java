package de.e2n.cdk.model;

import de.e2n.cdk.constructs.Policy;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Runtime;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Die Konfiguration einer AWS Lambda-Function. Wird per {@link Builder} initialisiert.
 */
public class DefaultLambdaConfig {

    private final String name;
    private final String handlerPath;
    private final Code lambdaCode;
    private final Runtime runtime;
    private final Map<String, PolicyDocument> inlinePolicies;
    private final Duration timeout;
    private final int memorySize;
    private final Size ephemeralStorageSize;
    private final Number reservedConcurrentExecutions;
    private final Architecture architecture;
    private Map<String, String> environment;
    private final String alias;

    private final Role role;
    private final IVpc vpc;
    private final SubnetSelection subnetSelection;
    private final List<ISecurityGroup> securityGroups;

    public DefaultLambdaConfig(String name,
                               String handlerPath,
                               Code lambdaCode,
                               Runtime runtime,
                               Map<String,PolicyDocument> inlinePolicies,
                               Duration timeout,
                               int memorySize,
                               Size ephemeralStorageSize,
                               Number reservedConcurrentExecutions,
                               Architecture architecture,
                               Map<String, String> environment,
                               String alias,
                               Role role,
                               IVpc vpc,
                               SubnetSelection subnetSelection,
                               List<ISecurityGroup> securityGroups) {
        this.name = name;
        this.handlerPath = handlerPath;
        this.lambdaCode = lambdaCode;
        this.runtime = runtime;
        this.inlinePolicies = inlinePolicies;
        this.timeout = timeout;
        this.memorySize = memorySize;
        this.ephemeralStorageSize = ephemeralStorageSize;
        this.reservedConcurrentExecutions = reservedConcurrentExecutions;
        this.architecture = architecture;
        this.environment = environment;
        this.alias = alias;
        this.role = role;
        this.vpc = vpc;
        this.subnetSelection = subnetSelection;
        this.securityGroups = securityGroups;
    }

    public String getName() {
        return name;
    }

    public String getHandlerPath() {
        return handlerPath;
    }

    public Code getLambdaCode() {
        return lambdaCode;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public Map<String,PolicyDocument> getInlinePolicies() {
        return inlinePolicies;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public Size getEphemeralStorageSize() {
        return ephemeralStorageSize;
    }

    public Number getReservedConcurrentExecutions() {
        return reservedConcurrentExecutions;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        if (environment == null) {
            this.environment = null;
        } else {
            this.environment = new HashMap<>(environment);
        }
    }

    public String getAlias() {
        return alias;
    }

    public Role getRole() {
        return role;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public SubnetSelection getSubnetSelection() {
        return subnetSelection;
    }

    public List<ISecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    /**
     * {@link DefaultLambdaConfig} Builder.
     */
    public static class Builder {

        private String name;
        private String handlerPath;
        private Code lambdaCode;
        private Runtime runtime = Runtime.JAVA_21;
        private Map<String, PolicyDocument> inlinePolicies = new LinkedHashMap<>();
        private Duration timeout = Duration.minutes(1);
        private int memorySize = 1408;
        private Size ephemeralStorageSize = Size.mebibytes(512);
        private Number reservedConcurrentExecutions;
        private Architecture architecture = Architecture.ARM_64;
        private Map<String, String> environment = null;
        private String alias = null;
        private Role role;
        private IVpc vpc;
        private SubnetSelection subnetSelection;
        private List<ISecurityGroup> securityGroups;

        Builder() {}

        /**
         *
         * @return {@link Builder} ein neuer Builder.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         *
         * @return {@link Builder} ein neuer Copy-Builder.
         */
        public static Builder create(DefaultLambdaConfig copy) {
            Builder builder = new Builder();
            builder.name = copy.getName();
            builder.handlerPath = copy.getHandlerPath();
            builder.lambdaCode = copy.getLambdaCode();
            builder.runtime = copy.getRuntime();
            builder.inlinePolicies = copy.getInlinePolicies();
            builder.timeout = copy.getTimeout();
            builder.memorySize = copy.getMemorySize();
            builder.ephemeralStorageSize = copy.getEphemeralStorageSize();
            builder.reservedConcurrentExecutions = copy.getReservedConcurrentExecutions();
            builder.architecture = copy.getArchitecture();
            builder.environment = copy.getEnvironment() == null ? null : new HashMap<>(copy.getEnvironment());
            builder.alias = copy.getAlias();
            builder.role = copy.getRole();
            builder.vpc = copy.getVpc();
            builder.subnetSelection = copy.getSubnetSelection();
            builder.securityGroups = copy.getSecurityGroups();
            return builder;
        }

        /**
         *
         * @param name Der Name der Lambda.
         * @return {@link Builder}
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Der Klassenpfad des Handlers.
         * @param handlerPath Der Pfad zum Handler.
         * @return {@link Builder}
         */
        public Builder handlerPath(String handlerPath) {
            this.handlerPath = handlerPath;
            return this;
        }

        /**
         * Der Code der Lambda. Zum Beispiel per {@link Code#fromAsset(String)}.
         * @param lambdaCode Der Code der Lambda.
         * @return {@link Builder}
         */
        public Builder lambdaCode(Code lambdaCode) {
            this.lambdaCode = lambdaCode;
            return this;
        }

        /**
         * The runtime environment for the Lambda function that you are uploading.
         * <p>
         * For valid values, see the Runtime property in the AWS Lambda Developer
         * Guide.
         * <p>
         * Use <code>Runtime.FROM_IMAGE</code> when defining a function from a Docker image.
         * <p>
         * @return {@code this}
         * @param runtime The runtime environment for the Lambda function that you are uploading. This parameter is required.
         */
        public Builder runtime(Runtime runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * @param policy Eine IAM-Policy, die als inline-Policy in der IAM-Role der Lambda gesetzt
         *               wird.
         * @return {@link Builder}
         */
        public Builder inlinePolicy(Policy policy) {
            this.inlinePolicies.put(policy.getName(), policy.getDocument());
            return this;
        }

        /**
         * Der Timeout der Lambda
         * @param timeout Timeout als Duration, max. 15 Minuten
         * @return {@link Builder}
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * The amount of memory, in MB, that is allocated to your Lambda function.
         * <p>
         * Lambda uses this value to proportionally allocate the amount of CPU
         * power. For more information, see Resource Model in the AWS Lambda
         * Developer Guide.
         * <p>
         * Default: 1408
         * Max: 10240
         * <p>
         * @param memorySize The amount of memory, in MB, that is allocated to your Lambda function.
         * @return {@link Builder}
         */
        public Builder memorySize(int memorySize) {
            this.memorySize = memorySize;
            return this;
        }

        /**
         * The size of the temporal storage, that is provided to each execution environment of your Lambda function.
         * <p>
         * Lambda provides ephemeral storage in the {@code /tmp} directory.
         * <p>
         * Default: 512 MB
         * Max: 10240 MB
         *
         * @param ephemeralStorageSize The size of ephemeral storage provided to your Lambda function execution environment.
         * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/configuration-ephemeral-storage.html">Configure ephemeral storage for Lambda functions</a>
         * @return {@link Builder}
         */
        public Builder ephemeralStorageSize(Size ephemeralStorageSize) {
            this.ephemeralStorageSize = ephemeralStorageSize;
            return this;
        }

        /**
         * The maximum of concurrent executions you want to reserve for the function.
         * <p>
         * Default: - No specific limit - account limit.
         * <p>
         * @return {@code this}
         * @see <a href="https://docs.aws.amazon.com/lambda/latest/dg/concurrent-executions.html">Understanding Lambda function scaling</a>
         * @param reservedConcurrentExecutions The maximum of concurrent executions you want to reserve for the function. This parameter is required.
         */
        public Builder reservedConcurrentExecutions(int reservedConcurrentExecutions) {
            this.reservedConcurrentExecutions = reservedConcurrentExecutions;
            return this;
        }

        /**
         * The system architectures compatible with this lambda function.
         * <p>
         * Default: Architecture.X86_64
         * <p>
         * @return {@code this}
         * @param architecture The system architectures compatible with this lambda function.
         */
        public Builder architecture(Architecture architecture) {
            this.architecture = architecture;
            return this;
        }

        /**
         * Fügt der Lambda-Umgebung eine Variable hinzu.
         * @param key Der Name der Umgebungsvariable.
         * @param value Der Wert der Umgebungsvariable.
         * @return {@link Builder}
         */
        public Builder environmentVariable(String key, String value) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Environment variable key must not be null or empty");
            }
            if (value == null) {
                throw new IllegalArgumentException("Environment variable value must not be null");
            }
            if( this.environment == null) {
                this.environment = new HashMap<>();
            }
            this.environment.put(key, value);
            return this;
        }


        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder vpc(IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public Builder subnetSelection(SubnetSelection subnetSelection) {
            this.subnetSelection = subnetSelection;
            return this;
        }

        public Builder securityGroups(List<ISecurityGroup> securityGroups) {
            this.securityGroups = securityGroups;
            return this;
        }

        /**
         *
         * @return {@link DefaultLambdaConfig}
         */
        public DefaultLambdaConfig build() {
            return new DefaultLambdaConfig(
                    name,
                    handlerPath,
                    lambdaCode,
                    runtime,
                    inlinePolicies,
                    timeout,
                    memorySize,
                    ephemeralStorageSize,
                    reservedConcurrentExecutions,
                    architecture,
                    environment,
                    alias,
                    role,
                    vpc,
                    subnetSelection,
                    securityGroups
            );
        }

    }
}
