package io.openems.edge.evcs.ocpp.alfen;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'applyChargeCurrentLimit'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "applyChargeCurrentLimit",
 *   "params": {
 *   	"connector": connectorId,
        "value": current_A
 *   }
 * }
 * </pre>
 */
public class ApplyChargeCurrentLimitRequest extends JsonrpcRequest {

	public static final String METHOD = "applyChargeCurrentLimit";
	private final int connectorId;
	private final int chargeCurrentLimit;

	/**
	 * Create {@link ApplyChargeCurrentLimitRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link ApplyChargeCurrentLimitRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ApplyChargeCurrentLimitRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var connectorId = JsonUtils.getAsInt(r.getParams(), "connector");
		var chargePowerLimit = JsonUtils.getAsInt(r.getParams(), "value");

		return new ApplyChargeCurrentLimitRequest(r, connectorId, chargePowerLimit);
	}

	public ApplyChargeCurrentLimitRequest(int connectorId, int chargeCurrentLimit) {
		super(METHOD);
		this.connectorId = connectorId;
		this.chargeCurrentLimit = chargeCurrentLimit;
	}

	private ApplyChargeCurrentLimitRequest(JsonrpcRequest request, int connectorId, int chargeCurrentLimit) {
		super(request, METHOD);
		this.connectorId = connectorId;
		this.chargeCurrentLimit = chargeCurrentLimit;
	}

	public int getChargeCurrentLimit() {
		return this.chargeCurrentLimit;
	}

	public int getConnectorId() {
		return this.connectorId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("connector", this.connectorId) //
				.addProperty("value", this.chargeCurrentLimit) //
				.build();
	}

}