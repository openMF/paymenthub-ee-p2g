package org.mifos.connector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan("org.mifos.connector.crm.properties")
@ComponentScan("org.mifos.connector.crm")
public class PheeConnectorCrmApplication {

    // Spring instantiates (and subclasses) this configuration class, so the constructor can't be private.
    protected PheeConnectorCrmApplication() {}

    public static void main(String[] args) {
        SpringApplication.run(PheeConnectorCrmApplication.class, args);
    }

}
