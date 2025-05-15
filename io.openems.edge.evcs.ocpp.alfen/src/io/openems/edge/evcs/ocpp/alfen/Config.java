package io.openems.edge.evcs.ocpp.alfen;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "OCPP EVCS Alfen Eve Single", //
		description = "Implements an OCPP capable Alfen Eve Single electric vehicle charging station.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "OCPP chargepoint identifier", description = "The OCPP identifier of the charging station.", required = true)
	String ocppId() default "";

	@AttributeDefinition(name = "OCPP connector identifier", description = "The connector id of the chargepoint (e.g. if there are two connectors, then the evcs has two id's 1 and 2).", required = true)
	int connectorId() default 0;

	// TODO check real world default values

	@AttributeDefinition(name = "Maximum power", description = "Maximum power of the charger in Watt.", required = true)
	int maxHwPower() default 22000;

	@AttributeDefinition(name = "Minimum power", description = "Minimum power of the charger in Watt.", required = true)
	int minHwPower() default 11000;

	String webconsole_configurationFactory_nameHint() default "EVCS OCPP Alfen [{id}]";
}
