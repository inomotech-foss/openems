package io.openems.edge.evcs.ocpp.alfen;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/** OSGi-Konfiguration für die Alfen Eve Single. */
@ObjectClassDefinition(
    name        = "OCPP EVCS Alfen Eve Single",
    description = "OpenEMS component for an Alfen Eve Single (OCPP 1.6-J).")
@interface Config {

    @AttributeDefinition(name = "Component-ID")
    String id() default "evcs0";

    @AttributeDefinition(name = "Alias")
    String alias() default "";

    @AttributeDefinition(name = "Enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "OCPP ChargePoint ID", required = true)
    String ocppId() default "";

    @AttributeDefinition(name = "OCPP connector identifier", description = "The connector id of the chargepoint (e.g. if there are two connectors, then the evcs has two id's 1 and 2).", required = true)
	int connectorId() default 0;

    @AttributeDefinition(name = "Maximum Hardware Power [W]", required = true)
    int maxHwPower() default 22000;

    @AttributeDefinition(name = "Minimum Hardware Power [W]", required = true)
    int minHwPower() default 6000;

    @AttributeDefinition(name = "Name-Hint",
        description = "Shown in Web-Console")
    String webconsole_configurationFactory_nameHint() default "EVCS Alfen [{id}]";
}