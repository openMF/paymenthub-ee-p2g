package org.mifos.connector.crm.zeebe;

import io.camunda.zeebe.client.api.worker.JobHandler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the workers make before they complete a job. Without them a missing variable threw a NullPointerException
 * inside the handler and the workflow waited with nothing in Operate.
 */
public final class JobGuards {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGuards.class);

    private JobGuards() {}

    /** Thrown by {@link #required} and turned into a failed job by {@link #failOnMissingVariables}. */
    public static final class MissingJobVariableException extends RuntimeException {

        public MissingJobVariableException(String name) {
            super("Missing job variable " + name);
        }
    }

    /** The variable as a string, or a {@link MissingJobVariableException} if it is absent. */
    public static String required(Map<String, Object> variables, String name) {
        Object value = variables.get(name);
        if (value == null) {
            throw new MissingJobVariableException(name);
        }
        return value.toString();
    }

    /**
     * Wraps a handler so a missing variable fails the job with retries 0 and names the variable: retrying cannot supply
     * it, and the incident then says what is wrong.
     */
    public static JobHandler failOnMissingVariables(JobHandler handler) {
        return (client, job) -> {
            try {
                handler.handle(client, job);
            } catch (MissingJobVariableException e) {
                LOGGER.error("Job '{}' with key {}: {}", job.getType(), job.getKey(), e.getMessage());
                client.newFailCommand(job.getKey()).retries(0).errorMessage(e.getMessage()).send().join();
            }
        };
    }
}
