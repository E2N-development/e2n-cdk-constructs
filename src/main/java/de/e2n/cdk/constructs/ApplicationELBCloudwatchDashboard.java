package de.e2n.cdk.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.cloudwatch.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * Construct for creating an Application-Elastic-Load-Balancer-Dashboard in CloudWatch.
 */
public class ApplicationELBCloudwatchDashboard extends Construct {

    private final Dashboard dashboard;

    private final String idLoadBalancer;

    /**
     * Creates a dashboard based on metrics of the referenced load balancer to monitor the application.
     * Included metrics are:
     * <ul>
     * <li><b>General information:</b> Number of new & active Connections</li>
     * <li><b>Request count:</b> Number of requests for which a target (e.g. an ecs service) was successfully determined</li>
     * <li><b>Response Codes:</b> Number of 2xx, 3xx, 4xx and 5xx HTTP codes returned by the target</li>
     * <li><b>Response Time:</b> Average time elapsed between request leaving load balancer and load balancer receiving response from target</li>
     * </ul>
     *
     * @param scope         determines the place in the construct tree
     * @param id            identifier for construct
     * @param dashboardName name of the dashboard to be created
     * @param loadBalancer  {@link ApplicationLoadBalancer} for which the dashboard is created
     */
    public ApplicationELBCloudwatchDashboard(final Construct scope, final String id, String dashboardName, ApplicationLoadBalancer loadBalancer) {
        super(scope, id);

        this.idLoadBalancer = loadBalancer.getLoadBalancerFullName();

        List<List<IWidget>> widgets = List.of(
                List.of(createInfoWidget(loadBalancer.getLoadBalancerArn())),
                createGeneralWidgets(),
                List.of(createRequestCountWidget()),
                List.of(createResponseCodesWidget()),
                List.of(createTargetResponseTimeWidget()));

        dashboard = Dashboard.Builder.create(this, "Dashboard")
                .dashboardName(dashboardName)
                .defaultInterval(Duration.hours(3))
                .widgets(widgets)
                .build();
    }

    private TextWidget createInfoWidget(String arnLoadBalancer) {
        return TextWidget.Builder.create()
                .width(24)
                .height(3)
                .markdown("Dashboard for monitoring standard metrics of landing zone via the `Load Balancer`\n"
                        + "- [Application Load Balancer](https://eu-central-1.console.aws.amazon.com/ec2/home?region=eu-central-1#LoadBalancer:loadBalancerArn=" + arnLoadBalancer + ";tab=listeners)\n"
                        + "- [AWS ELB Metrics Documentation](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-cloudwatch-metrics.html)\n"
                        + "```\nThis text was created in an " + this.getClass().getSimpleName() + "construct from the CDK\n```")
                .build();
    }

    private List<IWidget> createGeneralWidgets() {
        var newConnections = Metric.Builder.create()
                .metricName("NewConnectionCount")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.GREY)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .build();

        SingleValueWidget newConnectionsWidget = SingleValueWidget.Builder
                .create()
                .title("New Connections (Sum)")
                .width(12)
                .height(6)
                .sparkline(true)
                .metrics(List.of(newConnections))
                .build();

        var activeConnections = Metric.Builder.create()
                .metricName("ActiveConnectionCount")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.GREY)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .build();

        SingleValueWidget activeConnectionsWidget = SingleValueWidget.Builder
                .create()
                .title("Active Connections (Sum)")
                .width(12)
                .height(6)
                .sparkline(true)
                .metrics(List.of(activeConnections))
                .build();

        return List.of(newConnectionsWidget, activeConnectionsWidget);
    }

    private SingleValueWidget createRequestCountWidget() {
        var requestCount = Metric.Builder.create()
                .metricName("RequestCount")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .build();

        return SingleValueWidget.Builder
                .create()
                .title("Request Count (Sum)")
                .width(24)
                .height(6)
                .sparkline(true)
                .metrics(List.of(requestCount))
                .build();
    }

    private GraphWidget createResponseCodesWidget() {
        var success = Metric.Builder.create()
                .metricName("HTTPCode_Target_2XX_Count")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.GREEN)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .label("2xx")
                .build();
        var redirect = Metric.Builder.create()
                .metricName("HTTPCode_Target_3XX_Count")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.ORANGE)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .label("3xx")
                .build();
        var clientError = Metric.Builder.create()
                .metricName("HTTPCode_Target_4XX_Count")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.PURPLE)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .label("4xx")
                .build();
        var serverError = Metric.Builder.create()
                .metricName("HTTPCode_Target_5XX_Count")
                .namespace("AWS/ApplicationELB")
                .statistic("Sum")
                .color(Color.RED)
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .label("5xx")
                .build();

        return GraphWidget.Builder
                .create()
                .title("Response Codes (Sum)")
                .width(24)
                .liveData(true)
                .left(List.of(success, redirect, clientError, serverError))
                .build();
    }

    private GraphWidget createTargetResponseTimeWidget() {
        var requestCount = Metric.Builder.create()
                .metricName("TargetResponseTime")
                .namespace("AWS/ApplicationELB")
                .statistic(Stats.p(50))
                .dimensionsMap(Map.of("LoadBalancer", idLoadBalancer))
                .build();

        return GraphWidget.Builder
                .create()
                .title("Response Time (Median)")
                .width(24)
                .liveData(true)
                .left(List.of(requestCount))
                .build();
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

}
