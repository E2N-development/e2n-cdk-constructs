package de.e2n.cdk.constructs;

import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudwatch.Dashboard;
import software.amazon.awscdk.services.cloudwatch.GraphWidget;
import software.amazon.awscdk.services.cloudwatch.IWidget;
import software.amazon.awscdk.services.cloudwatch.MathExpression;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.TextWidget;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CognitoServiceQuotaCloudwatchDashboard extends Construct {

    private final Dashboard dashboard;
    private final List<MathExpression> serviceQuotaUtilizationMetrics;

    public CognitoServiceQuotaCloudwatchDashboard(final Construct scope,
                                                  final String id,
                                                  String namespace,
                                                  @Nullable TextWidget infoTextWidget) {
        super(scope, id);

        // % Utilization of service quotas
        // https://docs.aws.amazon.com/cognito/latest/developerguide/limits.html#category_operations
        List<String> serviceQuotaCategories = List.of("ClientAuthentication",
                                                      "UserAuthentication",
                                                      "UserToken",
                                                      "UserFederation",
                                                      "UserCreation",
                                                      "UserUpdate",
                                                      "UserRead",
                                                      "UserResourceRead",
                                                      "UserResourceUpdate",
                                                      "UserAccountRecovery");
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
                    .usingMetrics(Map.of("usage_data_" + category, callCount))
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
        widgets.add(List.of(serviceQuotaUtilGraph));

        dashboard = Dashboard.Builder.create(this, "CognitoServiceQuotaDashboard")
                .dashboardName("CognitoServiceQuotas")
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
