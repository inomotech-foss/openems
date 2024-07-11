package io.openems.edge.evcs.ocpp.alfen;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent.CurrentLimitResult;

/**
 * Represents a JSON-RPC Response for 'applyChargeCurrentLimit'.
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
public class ApplyChargeCurrentLimitResponse extends JsonrpcResponseSuccess {

	private final JsonPrimitive transactionAppliedSuccessfully;
	private final JsonPrimitive defaultAppliedSuccessfully;

	public ApplyChargeCurrentLimitResponse(CurrentLimitResult success) {
		this(UUID.randomUUID(), success);
	}

	public ApplyChargeCurrentLimitResponse(UUID id, CurrentLimitResult success) {
		super(id);
		this.transactionAppliedSuccessfully = new JsonPrimitive(success.transactionLimitSuccess());
		this.defaultAppliedSuccessfully = new JsonPrimitive(success.defaultLimitSuccess());
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("transactionAppliedSuccessfully", this.transactionAppliedSuccessfully).add("defaultAppliedSuccessfully", this.defaultAppliedSuccessfully) //
				.build();
	}

}