package io.openems.edge.evcs.ocpp.alfen;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'applyChargePowerLimit'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "meters": [
 *     	 {@link DiscovergyMeter#toJson()}
 *     ]
 *   }
 * }
 * </pre>
 */
public class ApplyChargePowerLimitResponse extends JsonrpcResponseSuccess {

	private final JsonPrimitive appliedSuccessfully;

	public ApplyChargePowerLimitResponse(boolean success) {
		this(UUID.randomUUID(), success);
	}

	public ApplyChargePowerLimitResponse(UUID id, boolean success) {
		super(id);
		this.appliedSuccessfully = new JsonPrimitive(success);
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("appliedSuccessfully", this.appliedSuccessfully) //
				.build();
	}

}