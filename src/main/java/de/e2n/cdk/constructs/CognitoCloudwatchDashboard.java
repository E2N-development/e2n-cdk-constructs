package de.e2n.cdk.constructs;

import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudwatch.*;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CognitoCloudwatchDashboard extends Construct {

    private final Dashboard dashboard;
    private final List<MathExpression> serviceQuotaUtilizationMetrics;

    public CognitoCloudwatchDashboard(final Construct scope,
                                      final String id,
                                      String dashboardName,
                                      String userPoolId,
                                      String userPoolClientId,
                                      String idpProviderName,
                                      String namespace,
                                      @Nullable TextWidget infoTextWidget) {
        super(scope, id);

        // https://docs.aws.amazon.com/cognito/latest/developerguide/metrics-for-cognito-user-pools.html
        var signInThrottles = Metric.Builder.create()
                .metricName("SignInThrottles")
                .namespace("AWS/Cognito")
                .statistic("Sum")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Throttled SignIns")
                .build();
        var signInSuccesses = Metric.Builder.create()
                .metricName("SignInSuccesses")
                .namespace("AWS/Cognito")
                .statistic("Sum")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Successful SignIns")
                .build();
        var signInTotal = Metric.Builder.create()
                .metricName("SignInSuccesses")
                .namespace("AWS/Cognito")
                .statistic("SampleCount")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Total SignIns")
                .build();
        var signInFailures = MathExpression.Builder.create()
                .expression("total-successes")
                .usingMetrics(Map.of("total", signInTotal, "successes", signInSuccesses))
                .label("Failed SignIns")
                .build();
        GraphWidget signInGraph = GraphWidget.Builder.create()
                .title("SignIn")
                .width(24)
                .statistic("SampleCount")
                .liveData(true)
                .left(List.of(signInSuccesses, signInTotal, signInFailures, signInThrottles))
                .build();

        // TokenRefreshes
        var tokenThrottles = Metric.Builder.create()
                .metricName("TokenRefreshThrottles")
                .namespace("AWS/Cognito")
                .statistic("Sum")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Throttled TokenRefreshes")
                .build();
        var tokenSuccesses = Metric.Builder.create()
                .metricName("TokenRefreshSuccesses")
                .namespace("AWS/Cognito")
                .statistic("Sum")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Successful TokenRefreshes")
                .build();
        var tokenTotal = Metric.Builder.create()
                .metricName("TokenRefreshSuccesses")
                .namespace("AWS/Cognito")
                .statistic("SampleCount")
                .dimensionsMap(Map.of("UserPool", userPoolId, "UserPoolClient", userPoolClientId))
                .label("Total TokenRefreshes")
                .build();
        var tokenFailures = MathExpression.Builder.create()
                .expression("total-successes")
                .usingMetrics(Map.of("total", tokenTotal, "successes", tokenSuccesses))
                .label("Failed TokenRefreshes")
                .build();
        GraphWidget tokenGraph = GraphWidget.Builder.create()
                .title("TokenRefreshes")
                .width(24)
                .statistic("SampleCount")
                .liveData(true)
                .left(List.of(tokenSuccesses, tokenFailures, tokenTotal, tokenThrottles))
                .build();

        // Federation
        GraphWidget federationGraph = null;
        if (idpProviderName != null) {
            var federationThrottles = Metric.Builder.create()
                    .metricName("FederationThrottles")
                    .namespace("AWS/Cognito")
                    .statistic("Sum")
                    .dimensionsMap(Map.of("UserPool",
                                                userPoolId,
                                                "UserPoolClient",
                                                userPoolClientId,
                                                "IdentityProvider",
                                                idpProviderName))
                    .label("Throttled Federations")
                    .build();
            var federationSuccesses = Metric.Builder.create()
                    .metricName("FederationSuccesses")
                    .namespace("AWS/Cognito")
                    .statistic("Sum")
                    .dimensionsMap(Map.of("UserPool",
                                                userPoolId,
                                                "UserPoolClient",
                                                userPoolClientId,
                                                "IdentityProvider",
                                                idpProviderName))
                    .label("Successful Federations")
                    .build();
            var federationTotal = Metric.Builder.create()
                    .metricName("FederationSuccesses")
                    .namespace("AWS/Cognito")
                    .statistic("SampleCount")
                    .dimensionsMap(Map.of("UserPool",
                                                userPoolId,
                                                "UserPoolClient",
                                                userPoolClientId,
                                                "IdentityProvider",
                                                idpProviderName))
                    .label("Total Federations")
                    .build();
            var federationFailures = MathExpression.Builder.create()
                    .expression("total-successes")
                    .usingMetrics(Map.of("total", federationTotal, "successes", federationSuccesses))
                    .label("Failed Federations")
                    .build();
            federationGraph = GraphWidget.Builder.create()
                    .title("Federation")
                    .width(24)
                    .statistic("SampleCount")
                    .liveData(true)
                    .left(List.of(federationSuccesses, federationFailures, federationTotal, federationThrottles))
                    .build();
        }

        // % Utilization of service quotas
        // https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html#category_operations
        List<String> serviceQuotaCategories = List.of("UserAuthentication",
                                                      "UserToken",
                                                      "UserFederation",
                                                      "UserCreation",
                                                      "UserUpdate",
                                                      "UserRead",
                                                      "UserResourceRead",
                                                      "UserResourceUpdate",
                                                      "UserAccountRecovery"); // ClientAuthentication data points missing
        serviceQuotaUtilizationMetrics = new ArrayList<>();

        for (String category : serviceQuotaCategories) {
            Metric callCount = Metric.Builder.create()
                    .metricName("CallCount")
                    .namespace("AWS/Usage")
                    .dimensionsMap(Map.of("Service",
                                                "Cognito User Pool",
                                                "Resource",
                                                category,
                                                "Class",
                                                "None",
                                                "Type",
                                                "API"))
                    .statistic("SampleCount")
                    .period(Duration.minutes(1))
                    .build();
            MathExpression utilization = MathExpression.Builder.create()
                    .expression("(usage_data_" + category + "/SERVICE_QUOTA(usage_data_" + category + "))*100")
                    .label(category)
                    .usingMetrics(Map.of("usage_data_" + category,
                                               callCount,
                                               "info_" + category,
                                               Metric.Builder.create()
                                                       .metricName("info_" + category)
                                                       .namespace(namespace)
                                                       .build()))
                    .period(Duration.minutes(1))
                    .build();
            serviceQuotaUtilizationMetrics.add(utilization);
        }

        GraphWidget serviceQuotaUtilGraph = GraphWidget.Builder.create()
                .title("Service Quota Utilizations in %")
                .width(24)
                .statistic("Sum")
                .liveData(true)
                .left(serviceQuotaUtilizationMetrics)
                .build();

        List<List<IWidget>> widgets = new ArrayList<>();
        if (infoTextWidget != null) {
            widgets.add(List.of(infoTextWidget));
        }
        widgets.add(List.of(signInGraph));
        widgets.add(List.of(tokenGraph));
        if (federationGraph != null) {
            widgets.add(List.of(federationGraph));
        }
        widgets.add(List.of(serviceQuotaUtilGraph));

        dashboard = Dashboard.Builder.create(this, "CognitoDashboard")
                .dashboardName(dashboardName)
                .defaultInterval(Duration.hours(3))
                .widgets(widgets)
                .build();

    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public List<MathExpression> getServiceQuotaUtilizationMetrics() {
        return serviceQuotaUtilizationMetrics;
    }

}
