package de.e2n.cdk.model;

import software.amazon.awscdk.Duration;
/**
 * Die Konfiguration einer AWS SQS-dead-letter-Queue. Wird per {@link Builder} initialisiert.
 */
public class DeadLetterQueueConfig {

    private final boolean enabledDeadLetterQueue;
    private final int maximumReceives;
    private final Duration receiveMessageWaitTime;
    private final Duration retentionPeriod;

    public DeadLetterQueueConfig(boolean enabledDeadLetterQueue, int maximumReceives, Duration receiveMessageWaitTime, Duration retentionPeriod) {
        this.enabledDeadLetterQueue = enabledDeadLetterQueue;
        this.maximumReceives = maximumReceives;
        this.receiveMessageWaitTime = receiveMessageWaitTime;
        this.retentionPeriod = retentionPeriod;
    }

    public boolean isEnabledDeadLetterQueue() {
        return enabledDeadLetterQueue;
    }

    public int getMaximumReceives() {
        return maximumReceives;
    }

    public Duration getReceiveMessageWaitTime() {
        return receiveMessageWaitTime;
    }

    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    /**
     * {@link DeadLetterQueueConfig} Builder.
     */
    public static class Builder {

        private boolean enabled = true;
        private int maximumReceives = 3;
        private Duration receiveMessageWaitTime = Duration.seconds(20);
        private Duration retentionPeriod = Duration.days(14);

        private Builder() {
        }

        /**
         * Deaktiviert die DeadLetterQueue
         * @return {@link DeadLetterQueueConfig.Builder}
         */
        public Builder disabled() {
            this.enabled = false;
            return this;
        }

        /**
         * @param maximumReceives Der Wert Maximale Empfangsvorgänge bestimmt, wann eine Nachricht an die DLQ gesendet wird.
         *                        Wenn der ReceiveCount-Wert für eine Nachricht die Anzahl der maximalen Empfangsvorgänge für eine Warteschlange überschreitet,
         *                        verschiebt Amazon SQS die Nachricht in eine zugeordnete DLQ (mit der ursprünglichen Nachrichten-ID).
         * @return {@link DeadLetterQueueConfig.Builder}
         */
        public Builder maximumReceives(int maximumReceives) {
            final int max = 1000;
            final int min = 1;
            this.maximumReceives = validateRange(maximumReceives, min, max);
            return this;
        }

        /**
         * @param receiveMessageWaitTime Die Wartezeit für das Empfangen von Nachrichten ist die maximale Zeitspanne, die Abfragen darauf warten, dass Nachrichten empfangen werden.
         *                Der Mindestwert beträgt Null und der Höchstwert beträgt 20 Sekunden.
         * @return {@link DeadLetterQueueConfig.Builder}
         */
        public Builder receiveMessageWaitTime(Duration receiveMessageWaitTime) {
            final Duration max = Duration.seconds(20);
            final Duration min = Duration.seconds(0);
            this.receiveMessageWaitTime = validateRange(receiveMessageWaitTime, min, max);
            return this;
        }

        /**
         * @param retentionPeriod Der Aufbewahrungszeitraum für Nachrichten ist die Zeitspanne, die Amazon SQS Nachrichten beibehält, wenn sie nicht gelöscht werden.
         *                        Amazon SQS löscht automatisch Nachrichten, die sich über den maximalen Aufbewahrungszeitraum für Nachrichten hinaus in einer Warteschlange befunden haben.
         *                        Der Standardaufbewahrungszeitraum beträgt 4 Tage. Der Aufbewahrungszeitraum kann 60 Sekunden bis 1 209 600 Sekunden (14 Tage) umfassen.
         * @return {@link DeadLetterQueueConfig.Builder}
         */
        public Builder retentionPeriod(Duration retentionPeriod) {
            final Duration max = Duration.days(14);
            final Duration min = Duration.minutes(1);
            this.receiveMessageWaitTime = validateRange(retentionPeriod, min, max);
            return this;
        }


        private int validateRange(int value, int min, int max) {
            if (value > max) return max;
            if (value < min) return min;
            return value;
        }

        private Duration validateRange(Duration value, Duration min, Duration max) {
            if (value.toSeconds().intValue() > max.toSeconds().intValue()) return max;
            if (value.toSeconds().intValue() < min.toSeconds().intValue()) return min;
            return value;
        }

        /**
         * @return {@link DeadLetterQueueConfig.Builder} ein neuer Builder.
         */
        public static Builder create() {
            return new Builder();
        }


        /**
         * @return {@link DeadLetterQueueConfig}
         */
        public DeadLetterQueueConfig build() {
            return new DeadLetterQueueConfig(enabled, maximumReceives, receiveMessageWaitTime, retentionPeriod);
        }

    }

}
