package de.e2n.cdk.constructs;

import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.MathExpression;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.sns.ITopic;
import software.constructs.Construct;

import java.util.List;

public class CognitoServiceQuotaAlarms extends Construct {

    public CognitoServiceQuotaAlarms(
            Construct scope,
            String id,
            ITopic alarmTopic,
            Number threshold,
            List<MathExpression> metrics) {
        super(scope, id);

        for (MathExpression metric : metrics) {
            var errorAlarm = Alarm.Builder.create(this, metric.getLabel() + "ErrorAlarm")
                    .metric(metric)
                    .alarmDescription("The service quota " + metric.getLabel() + " exceeds " + threshold + "% utilization.")
                    .comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
                    .threshold(threshold)
                    .datapointsToAlarm(10)
                    .evaluationPeriods(10)
                    .treatMissingData(TreatMissingData.MISSING)
                    .build();
            errorAlarm.addAlarmAction(new SnsAction(alarmTopic));
        }
    }

}
