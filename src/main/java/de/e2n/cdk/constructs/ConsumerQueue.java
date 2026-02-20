package de.e2n.cdk.constructs;

import de.e2n.cdk.model.DeadLetterQueueConfig;
import de.e2n.cdk.model.ILambda;
import de.e2n.cdk.model.QueueConfig;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.lambda.CfnEventSourceMapping;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.constructs.Construct;

/**
 * Diese Klasse konstruiert mittels AWS CDK eine SQS-Queue und eine Consumer-Lambda.
 * Die erzeugte Consumer-Lambda wird von neuen Nachrichten in der Queue getriggert und liest diese.
 *
 *  Queue --> Consumer
 *
 * Das CDK-Konstrukt besteht aus den folgenden AWS Ressourcen:
 * - AWS Lambda Function inklusive IAM Role, Version & Alias
 * - eine SQS Queue + Deadletter-Queue
 */
public class ConsumerQueue extends Construct {

    private final Queue queue;
    private final ILambda consumer;

    public ConsumerQueue(final Construct scope,
                         final String id,
                         ILambda consumer,
                         QueueConfig queueConfig) {
        super(scope, id);
        this.consumer = consumer;

        Queue.Builder queueBuilder = Queue.Builder.create(this, "Queue")
                .removalPolicy(RemovalPolicy.RETAIN)
                .encryption(QueueEncryption.KMS_MANAGED)
                .visibilityTimeout(consumer.getFunction().getTimeout().plus(Duration.minutes(1)))
                .receiveMessageWaitTime(Duration.seconds(20)); // long polling, max value is 20 sec.

        // https://github.com/aws/aws-cdk/issues/8550
        if (queueConfig.isFifo()) {
            queueBuilder.fifo(queueConfig.isFifo())
                    .contentBasedDeduplication(queueConfig.isContentBasedDeduplication());
        }

        // dead-letter queue configuration
        final DeadLetterQueueConfig deadLetterQueueConfig = queueConfig.getDeadLetterQueueConfig();

        if (deadLetterQueueConfig.isEnabledDeadLetterQueue()) {

            Queue.Builder deadletterQueueBuilder = Queue.Builder.create(this, "DeadletterQueue")
                    .removalPolicy(RemovalPolicy.RETAIN)
                    .encryption(QueueEncryption.KMS_MANAGED)
                    .receiveMessageWaitTime(deadLetterQueueConfig.getReceiveMessageWaitTime())
                    .retentionPeriod(deadLetterQueueConfig.getRetentionPeriod());

            if (queueConfig.isFifo()) {
                deadletterQueueBuilder.fifo(queueConfig.isFifo())
                        .contentBasedDeduplication(queueConfig.isContentBasedDeduplication());
            }

            var deadletterQueue = deadletterQueueBuilder.build();
            queue = queueBuilder.deadLetterQueue(DeadLetterQueue.builder()
                            .queue(deadletterQueue)
                            .maxReceiveCount(deadLetterQueueConfig.getMaximumReceives())
                            .build())
                    .build();
        } else {
            queue = queueBuilder.build();
        }


        queue.grantConsumeMessages(this.consumer.getAlias());

        // consumer.getAlias().addEventSource(SqsEventSource.Builder
        //                                           .create(queue)
        //                                           .batchSize(queueConfig.getBatchSize())
        //                                           .build());

        // Workaround for https://github.com/aws/serverless-application-model/issues/1320
        CfnEventSourceMapping cfnEventSourceMapping = CfnEventSourceMapping.Builder
                .create(this, "CfnEventSourceMapping")
                .functionName(this.consumer.getAlias().getFunctionName())
                .batchSize(queueConfig.getBatchSize())
                .eventSourceArn(queue.getQueueArn())
                .build();
    }

    public Queue getQueue() {
        return queue;
    }

    public ILambda getConsumer() {
        return consumer;
    }

}

