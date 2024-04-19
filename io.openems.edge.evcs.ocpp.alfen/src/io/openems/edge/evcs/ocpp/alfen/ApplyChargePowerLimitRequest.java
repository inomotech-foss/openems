package io.openems.edge.evcs.ocpp.alfen;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'applyChargePowerLimit'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "applyChargePowerLimit",
 *   "params": {
 *   	"connector": <connectorId>,
        "value": <power_W>
 *   }
 * }
 * </pre>
 */
public class ApplyChargePowerLimitRequest extends JsonrpcRequest {

	public static final String METHOD = "applyChargePowerLimit";
	private final int connectorId;
	private final int chargePowerLimit;

	/**
	 * Create {@link ApplyChargePowerLimitRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link ApplyChargePowerLimitRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ApplyChargePowerLimitRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var connectorId = JsonUtils.getAsInt(r.getParams(), "connector");
		var chargePowerLimit = JsonUtils.getAsInt(r.getParams(), "value");

		return new ApplyChargePowerLimitRequest(r, connectorId, chargePowerLimit);
	}

	public ApplyChargePowerLimitRequest(int connectorId, int chargePowerLimit) {
		super(METHOD);
		this.connectorId = connectorId;
		this.chargePowerLimit = chargePowerLimit;
	}

	private ApplyChargePowerLimitRequest(JsonrpcRequest request, int connectorId, int chargePowerLimit) {
		super(request, METHOD);
		this.connectorId = connectorId;
		this.chargePowerLimit = chargePowerLimit;
	}

	public int getChargePowerLimit() {
		return this.chargePowerLimit;
	}

	public int getConnectorId() {
		return this.connectorId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("connector", this.connectorId) //
				.addProperty("value", this.chargePowerLimit) //
				.build();
	}

}