package io.openems.edge.evcs.ocpp.alfen;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.ChargingProfile;
import eu.chargetime.ocpp.model.core.ChargingProfileKindType;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.model.core.ChargingRateUnitType;
import eu.chargetime.ocpp.model.core.ChargingSchedule;
import eu.chargetime.ocpp.model.core.ChargingSchedulePeriod;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(
        name               = "Evcs.Ocpp.Alfen.EveSingle",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate           = true)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE,
               EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE })
public class EvcsOcppAlfenEveSingleImpl extends AbstractManagedOcppEvcsComponent
        implements EvcsOcppAlfenEveSingle, Evcs, MeasuringEvcs, ManagedEvcs,
                   ElectricityMeter, OpenemsComponent, EventHandler, SocEvcs, ComponentJsonApi {

    /* ------------------------------------------------------------------ */
    /* Konstanten                                                         */
    /* ------------------------------------------------------------------ */
    private static final int  U_NOMINAL_V      = 230;
    private static final int  FALLBACK_LIMIT_W = 32 * U_NOMINAL_V * 3; // 22 kW
    private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE //
	};                                   
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<>(//
			Arrays.asList(//
					OcppInformations.values()) //
	);

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ComponentManager componentManager;

	private static final io.openems.edge.common.channel.ChannelId[] CHANNELS;
	static {
	    Map<String, io.openems.edge.common.channel.ChannelId> uniq = new LinkedHashMap<>();

	    // Helfer-Methode
	    Consumer<io.openems.edge.common.channel.ChannelId[]> add =
	        arr -> {
	            for (var c : arr) {
	                String key = ((Enum<?>) c).name();   // "FREQUENCY", "VOLTAGE" …
	                uniq.putIfAbsent(key, c);            // überschreibt nicht, falls Name schon da
	            }
	        };

	    add.accept(OpenemsComponent.ChannelId.values());
	    add.accept(ElectricityMeter.ChannelId.values());
	    add.accept(Evcs.ChannelId.values());
	    add.accept(ManagedEvcs.ChannelId.values());
	    add.accept(MeasuringEvcs.ChannelId.values());
	    add.accept(SocEvcs.ChannelId.values());
	    add.accept(EvcsOcppAlfenEveSingle.ChannelId.values());

	    CHANNELS = uniq.values().toArray(new io.openems.edge.common.channel.ChannelId[0]);
	}

	public EvcsOcppAlfenEveSingleImpl() {
	    super(PROFILE_TYPES, CHANNELS);   // nur noch EIN Array → garantiert ohne Duplikate
	}

    @Activate
    private void activate(ComponentContext ctx, Config cfg) {
        this.config = cfg;
        super.activate(ctx, cfg.id(), cfg.alias(), cfg.enabled());

        this._setChargingType(ChargingType.AC);
        this._setPhases(Phases.THREE_PHASE);
        this._setPowerPrecision(U_NOMINAL_V);
        this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
        this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
    }

    @Deactivate
    protected void deactivate() { super.deactivate(); }

    /* ------------------------------------------------------------------ */
    /* OpenEMS - Basis-Implementierungen                                  */
    /* ------------------------------------------------------------------ */
    @Override public void handleEvent(Event event) { super.handleEvent(event); }
    @Override public Set<OcppInformations> getSupportedMeasurements() { return MEASUREMENTS; }

    /* Konfiguration */
    @Override public String  getConfiguredOcppId()      { return config.ocppId(); }
    @Override public Integer getConfiguredConnectorId() { return config.connectorId(); }

    /* Limits */
    @Override public int getConfiguredMinimumHardwarePower() { return config.minHwPower(); }
    @Override public int getConfiguredMaximumHardwarePower() { return config.maxHwPower(); }
    @Override public int getMinimumTimeTillChargingLimitTaken() { return 30; }

    /* Meter-Infos */
    @Override public MeterType     getMeterType()     { return MeterType.CONSUMPTION_METERED; }
    @Override public PhaseRotation getPhaseRotation() { return PhaseRotation.L1_L2_L3; }

    /* Services */
    @Override public Timedata  getTimedata()  { return timedata; }
    @Override public EvcsPower getEvcsPower() { return evcsPower; }

    /* EVCS-spezifisch */
    @Override public boolean returnsSessionEnergy() { return false; }
    @Override public boolean getConfiguredDebugMode() { return false; }
    

    /* ------------------------------------------------------------------ */
    /* OCPP-Standard-Requests                                             */
    /* ------------------------------------------------------------------ */
    @Override
    public OcppStandardRequests getStandardRequests() {
        final AbstractManagedOcppEvcsComponent self = this;

        return new OcppStandardRequests() {

            @Override
            public Request setChargePowerLimit(int powerW) {
                int phases  = self.getPhasesAsInt();
                int targetA = (int)Math.round(powerW / (phases * (double)U_NOMINAL_V));

                int maxA = self.getMaximumHardwarePower()
                               .orElse(FALLBACK_LIMIT_W) / (phases * U_NOMINAL_V);

                targetA = Math.max(6, Math.min(targetA, maxA));           // Alfen fordert ≥ 6 A
                return new ChangeConfigurationRequest("Station-MaxCurrent",
                                                      String.valueOf(targetA));
            }

            @Override
            public Request setChargeCurrentLimit(int currentA) {
                ChargingSchedulePeriod[] p = { new ChargingSchedulePeriod(0, (double)currentA) };
                ChargingProfile profile = new ChargingProfile(
                        1, 4, ChargingProfilePurposeType.ChargePointMaxProfile,
                        ChargingProfileKindType.Absolute,
                        new ChargingSchedule(ChargingRateUnitType.A, p));
                return new SetChargingProfileRequest(0, profile);          // 0 == gesamte Säule
            }

            @Override
            public Request setTxProfile(int connector, int currentA) {
                if (connector != getConfiguredConnectorId()) return null;
                ChargingSchedulePeriod[] p = { new ChargingSchedulePeriod(0, (double)currentA) };
                ChargingProfile prof = new ChargingProfile(
                        1, 5, ChargingProfilePurposeType.TxProfile,
                        ChargingProfileKindType.Absolute,
                        new ChargingSchedule(ChargingRateUnitType.A, p));
                return new SetChargingProfileRequest(connector, prof);
            }

            @Override
            public Request setTxDefaultProfile(int connector, int currentA) {
                ChargingSchedulePeriod[] p = { new ChargingSchedulePeriod(0, (double)currentA) };
                ChargingProfile prof = new ChargingProfile(
                        1, 5, ChargingProfilePurposeType.TxDefaultProfile,
                        ChargingProfileKindType.Absolute,
                        new ChargingSchedule(ChargingRateUnitType.A, p));
                return new SetChargingProfileRequest(connector, prof);
            }

            @Override public Request setDisplayText(String text) { return null; }
            
            
        };
    }

    /* ------------------------------------------------------------------ */
    /* OCPP-Initial- & periodische Requests                                */
    /* ------------------------------------------------------------------ */
    @Override
    public List<Request> getRequiredRequestsAfterConnection() {
        return List.of(
            new ChangeConfigurationRequest("MeterValueSampleInterval", "10"),
            new ChangeConfigurationRequest("MeterValuesSampledData",
                "Current.Import,Voltage,Power.Active.Import,Current.Offered,Energy.Active.Import.Register"),
            new ChangeConfigurationRequest("ClockAlignedDataInterval", "10"),
            new ChangeConfigurationRequest("MeterValuesAlignedData",
                "Energy.Active.Import.Register,Current.Import,Power.Active.Import,Voltage,Current.Offered"),
            new ChangeConfigurationRequest("SendStationStatus", "True")
        );
    }

    @Override public List<Request> getRequiredRequestsDuringConnection() { return List.of(); }

    @Override
    public void buildJsonApiRoutes(JsonApiBuilder builder) {
        /* 1) applyChargeCurrentLimit */
        builder.handleRequest(ApplyChargeCurrentLimitRequest.METHOD, call -> {
            var req = ApplyChargeCurrentLimitRequest.from(call.getRequest());
            var res = applyChargeCurrentLimit(req.connectorId, req.chargeCurrentLimit);
            return new ApplyChargeCurrentLimitResponse(res);   // <-- Response zurückgeben
        });

        /* 2) applyChargePowerLimit  (nur 1 Parameter) */
        builder.handleRequest(ApplyChargePowerLimitRequest.METHOD, call -> {
            var req = ApplyChargePowerLimitRequest.from(call.getRequest());
            boolean ok = applyChargePowerLimit(req.chargePowerLimit);    // <-- nur Leistung in W
            return new ApplyChargePowerLimitResponse(ok);      // <-- Response zurückgeben
        });
    }

    /* ############################################################### */
    /*     ----------   JSON-RPC Helper-Klassen  ----------            */
    /* ############################################################### */

    /* ---------- applyChargeCurrentLimit (Request) ------------------ */
    public static class ApplyChargeCurrentLimitRequest extends JsonrpcRequest {
        public static final String METHOD = "applyChargeCurrentLimit";
        private final int connectorId;
        private final int chargeCurrentLimit;
        public static ApplyChargeCurrentLimitRequest from(JsonrpcRequest r)
                throws OpenemsNamedException {
            int conn = JsonUtils.getAsInt(r.getParams(), "connector");
            int val  = JsonUtils.getAsInt(r.getParams(), "value");
            return new ApplyChargeCurrentLimitRequest(r, conn, val);
        }
        public ApplyChargeCurrentLimitRequest(int connectorId, int limitA) {
            super(METHOD);
            this.connectorId = connectorId;
            this.chargeCurrentLimit = limitA;
        }
        private ApplyChargeCurrentLimitRequest(JsonrpcRequest t,
                                               int connectorId, int limitA) {
            super(t, METHOD);
            this.connectorId = connectorId;
            this.chargeCurrentLimit = limitA;
        }
        public int getConnectorId()        { return connectorId; }
        public int getChargeCurrentLimit() { return chargeCurrentLimit; }
        @Override public JsonObject getParams() {
            return JsonUtils.buildJsonObject()
                    .addProperty("connector", connectorId)
                    .addProperty("value", chargeCurrentLimit)
                    .build();
        }
    }

    /* ---------- applyChargeCurrentLimit (Response) ----------------- */
    public static class ApplyChargeCurrentLimitResponse
            extends JsonrpcResponseSuccess {
        private final JsonPrimitive txSuccess;
        private final JsonPrimitive defaultSuccess;
        public ApplyChargeCurrentLimitResponse(
                AbstractManagedOcppEvcsComponent.CurrentLimitResult res) {
            this(UUID.randomUUID(), res);
        }
        public ApplyChargeCurrentLimitResponse(UUID id,
                AbstractManagedOcppEvcsComponent.CurrentLimitResult res) {
            super(id);
            this.txSuccess      = new JsonPrimitive(res.transactionLimitSuccess());
            this.defaultSuccess = new JsonPrimitive(res.defaultLimitSuccess());
        }
        @Override public JsonObject getResult() {
            return JsonUtils.buildJsonObject()
                    .add("transactionAppliedSuccessfully", txSuccess)
                    .add("defaultAppliedSuccessfully",      defaultSuccess)
                    .build();
        }
    }

    /* ---------- applyChargePowerLimit (Request) -------------------- */
    public static class ApplyChargePowerLimitRequest extends JsonrpcRequest {
        public static final String METHOD = "applyChargePowerLimit";
        private final int connectorId;
        private final int chargePowerLimit;
        public static ApplyChargePowerLimitRequest from(JsonrpcRequest r)
                throws OpenemsNamedException {
            int conn = JsonUtils.getAsInt(r.getParams(), "connector");
            int val  = JsonUtils.getAsInt(r.getParams(), "value");
            return new ApplyChargePowerLimitRequest(r, conn, val);
        }
        public ApplyChargePowerLimitRequest(int connectorId, int limitW) {
            super(METHOD);
            this.connectorId = connectorId;
            this.chargePowerLimit = limitW;
        }
        private ApplyChargePowerLimitRequest(JsonrpcRequest t,
                                             int connectorId, int limitW) {
            super(t, METHOD);
            this.connectorId = connectorId;
            this.chargePowerLimit = limitW;
        }
        public int getConnectorId()      { return connectorId; }
        public int getChargePowerLimit() { return chargePowerLimit; }
        @Override public JsonObject getParams() {
            return JsonUtils.buildJsonObject()
                    .addProperty("connector", connectorId)
                    .addProperty("value", chargePowerLimit)
                    .build();
        }
    }

    /* ---------- applyChargePowerLimit (Response) ------------------- */
    public static class ApplyChargePowerLimitResponse
            extends JsonrpcResponseSuccess {
        private final JsonPrimitive success;
        public ApplyChargePowerLimitResponse(boolean ok) {
            this(UUID.randomUUID(), ok);
        }
        public ApplyChargePowerLimitResponse(UUID id, boolean ok) {
            super(id);
            this.success = new JsonPrimitive(ok);
        }
        @Override public JsonObject getResult() {
            return JsonUtils.buildJsonObject()
                    .add("appliedSuccessfully", success).build();
        }
    }
}