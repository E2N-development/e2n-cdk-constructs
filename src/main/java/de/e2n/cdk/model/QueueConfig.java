package de.e2n.cdk.model;

/**
 * Die Konfiguration einer AWS SQS-Queue. Wird per {@link Builder} initialisiert.
 */
public class QueueConfig {

    private final int batchSize;
    private final boolean fifo;
    private final boolean contentBasedDeduplication;

    private final DeadLetterQueueConfig deadLetterQueueConfig;

    public QueueConfig(int batchSize, boolean fifo, boolean contentBasedDeduplication, DeadLetterQueueConfig deadLetterQueueConfig) {
        this.batchSize = batchSize;
        this.fifo = fifo;
        this.contentBasedDeduplication = contentBasedDeduplication;
        this.deadLetterQueueConfig = deadLetterQueueConfig;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isFifo() {
        return fifo;
    }

    public boolean isContentBasedDeduplication() {
        return contentBasedDeduplication;
    }

    public DeadLetterQueueConfig getDeadLetterQueueConfig() {
        return deadLetterQueueConfig;
    }

    /**
     * {@link QueueConfig} Builder.
     */
    public static class Builder {

        private boolean fifo = false;
        private boolean contentBasedDeduplication = false;
        private int batchSize = 5;

        private DeadLetterQueueConfig deadLetterQueueConfig = DeadLetterQueueConfig.Builder.create().build(); // gets initialized with default values

        private Builder() {}

        /**
         *
         * @param batchSize die Anzahl der Messages, die je SQS-Event an die Consumer-Lambda
         *                  gesendet werden soll. Default: 5
         * @return {@link QueueConfig.Builder}
         */
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         *
         * @return {@link QueueConfig.Builder} ein neuer Builder.
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         *
         * @param fifo Flag, ob die SQS-Queue vom Typ FIFO sein soll. Default: false
         * @return {@link QueueConfig.Builder}
         */
        public Builder fifo(boolean fifo) {
            this.fifo = fifo;
            return this;
        }

        /**
         * Specifies whether to enable content-based deduplication.
         * <p>
         * During the deduplication interval (5 minutes), Amazon SQS treats
         * messages that are sent with identical content (excluding attributes) as
         * duplicates and delivers only one copy of the message.
         * <p>
         * If you don't enable content-based deduplication and you want to deduplicate
         * messages, provide an explicit deduplication ID in your SendMessage() call.
         * <p>
         * (Only applies to FIFO queues.)
         * <p>
         * Default: false
         * <p>
         * @param contentBasedDeduplication Specifies whether to enable content-based deduplication.
         * @return {@link QueueConfig.Builder}
         */
        public Builder contentBasedDeduplication(boolean contentBasedDeduplication) {
            this.contentBasedDeduplication = contentBasedDeduplication;
            return this;
        }

        public Builder deadLetterQueueConfig(DeadLetterQueueConfig deadLetterQueueConfig) {
            this.deadLetterQueueConfig = deadLetterQueueConfig;
            return this;
        }

        /**
         *
         * @return {@link QueueConfig}
         */
        public QueueConfig build() {
            return new QueueConfig(batchSize, fifo, contentBasedDeduplication, deadLetterQueueConfig);
        }

    }

}
