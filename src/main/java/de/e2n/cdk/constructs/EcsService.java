package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.List;

public class EcsService {

    public static FargateTaskDefinition fargateTaskDefinition(Construct scope, TaskDefinitionProps props) {
        var runtimePlatform = RuntimePlatform.builder()
                .operatingSystemFamily(OperatingSystemFamily.LINUX)
                .cpuArchitecture(CpuArchitecture.ARM64)
                .build();

        // Task Execution Role to pull images from ECR
        var taskExecutionRole = Role.Builder.create(scope, props.id + "-taskExecutionRole")
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")))
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .build();

        var fargateTaskDefinition = FargateTaskDefinition.Builder.create(scope, props.id)
                .family(props.id)
                .runtimePlatform(runtimePlatform)
                .executionRole(taskExecutionRole)
                .taskRole(props.taskRole)
                .cpu(props.cpu)
                .memoryLimitMiB(props.memory)
                .build();

        int i = 0;
        for (ContainerDefinitionOptions definition : props.containerDefinitionOptions) {
            fargateTaskDefinition.addContainer(props.id + "-" + definition.getContainerName() + "-" + i++, definition);
        }

        return fargateTaskDefinition;
    }

    public static class TaskDefinitionProps {

        private final String id;
        private final IRole taskRole;
        private final Number cpu;
        private final Number memory;
        private final List<ContainerDefinitionOptions> containerDefinitionOptions;

        public TaskDefinitionProps(String id,
                                   IRole taskRole,
                                   Number cpu,
                                   Number memory,
                                   List<ContainerDefinitionOptions> containerDefinitionOptions) {
            this.id = id;
            this.taskRole = taskRole;
            this.cpu = cpu;
            this.memory = memory;
            this.containerDefinitionOptions = containerDefinitionOptions;
        }

        public String getId() {
            return id;
        }

        public IRole getTaskRole() {
            return taskRole;
        }

        public Number getCpu() {
            return cpu;
        }

        public Number getMemory() {
            return memory;
        }

        public List<ContainerDefinitionOptions> getContainerDefinitionOptions() {
            return containerDefinitionOptions;
        }

    }

    public static class ServiceProps {

        private final String id;
        private final ApplicationLoadBalancer loadBalancer;
        private final Certificate certificate;
        private final Cluster fargateCluster;
        private final  int loadBalancerPort;
        private final  int targetGroupPort;
        private final  String healthCheckPath;

        public ServiceProps(String id,
                            ApplicationLoadBalancer loadBalancer,
                            Certificate certificate,
                            Cluster fargateCluster,
                            int loadBalancerPort,
                            int targetGroupPort,
                            String healthCheckPath) {
            this.id = id;
            this.loadBalancer = loadBalancer;
            this.certificate = certificate;
            this.fargateCluster = fargateCluster;
            this.loadBalancerPort = loadBalancerPort;
            this.targetGroupPort = targetGroupPort;
            this.healthCheckPath = healthCheckPath;
        }

        public String getId() {
            return id;
        }

        public ApplicationLoadBalancer getLoadBalancer() {
            return loadBalancer;
        }

        public Certificate getCertificate() {
            return certificate;
        }

        public Cluster getFargateCluster() {
            return fargateCluster;
        }

        public int getLoadBalancerPort() {
            return loadBalancerPort;
        }

        public int getTargetGroupPort() {
            return targetGroupPort;
        }

        public String getHealthCheckPath() {
            return healthCheckPath;
        }

    }

}
