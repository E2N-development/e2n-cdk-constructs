package de.e2n.cdk.constructs;

import de.e2n.cdk.model.ILambda;
import de.e2n.cdk.model.QueueConfig;
import software.constructs.Construct;
import software.amazon.awscdk.services.events.Schedule;

import java.util.List;

/**
 * Diese Klasse konstruiert mittels AWS CDK eine Schedule getriggerte Producer-Lambda, SQS-Queue
 * und Consumer-Lambda.
 * Die sogenannte Producer-Lambda ist in der Lage, Nachrichten in die SQS-Queue zu legen/versenden.
 * Die ebenfalls erzeugte Consumer-Lambda wird von der Queue getriggert und liest die Nachrichten.
 *
 * Schedule --> Producer --> Queue --> Consumer
 *
 * Das CDK-Konstrukt besteht aus den folgenden AWS Ressourcen:
 * - 2x AWS Lambda Functions inklusive IAM Role, Version & Alias (Producer + Consumer)
 * - eine EventBridge Schedule-Rule
 * - eine SQS Queue + Deadletter-Queue
 */
public class ScheduledProducerConsumerQueue extends Construct {

    private final ScheduledLambda producer;
    private final ConsumerQueue consumerQueue;

    public ScheduledProducerConsumerQueue(final Construct scope,
                                          final String id,
                                          Schedule schedule,
                                          ILambda producer,
                                          ILambda consumer,
                                          QueueConfig queueConfig) {
        this(scope, id, List.of(schedule), producer, consumer, queueConfig);
    }

    public ScheduledProducerConsumerQueue(final Construct scope,
                                          final String id,
                                          List<Schedule> schedules,
                                          ILambda producer,
                                          ILambda consumer,
                                          QueueConfig queueConfig) {
        super(scope, id);

        this.producer = new ScheduledLambda(this,
                                       "Producer",
                                       producer,
                                       schedules);
        consumerQueue = new ConsumerQueue(this,
                                          "ConsumerQueue",
                                          consumer,
                                          queueConfig);
        consumerQueue.getQueue().grantSendMessages(this.producer.getLambda().getAlias());
        this.producer.getLambda()
                .getFunction()
                .addEnvironment("QUEUE_URL", consumerQueue.getQueue().getQueueUrl());
    }


    public ScheduledLambda getProducer() {
        return producer;
    }

    public ConsumerQueue getConsumerQueue() {
        return consumerQueue;
    }

}
