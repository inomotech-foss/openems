package io.openems.edge.evcs.ocpp.alfen;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.ChargingProfile;
import eu.chargetime.ocpp.model.core.ChargingProfileKindType;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.model.core.ChargingRateUnitType;
import eu.chargetime.ocpp.model.core.ChargingSchedule;
import eu.chargetime.ocpp.model.core.ChargingSchedulePeriod;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.types.MeterType;
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
                   ElectricityMeter, OpenemsComponent, EventHandler {

    /* ------------------------------------------------------------------ */
    /* Konstanten                                                         */
    /* ------------------------------------------------------------------ */
    private static final int  U_NOMINAL_V      = 230;
    private static final int  FALLBACK_LIMIT_W = 32 * U_NOMINAL_V * 3; // 22 kW
    private static final OcppProfileType[] PROFILES = { OcppProfileType.CORE };

    /* Kanäle ohne Duplikate                                             */
    private static final io.openems.edge.common.channel.ChannelId[] CHANNELS;
    static {
        Set<io.openems.edge.common.channel.ChannelId> set = new LinkedHashSet<>();
        Collections.addAll(set, OpenemsComponent.ChannelId.values());
        Collections.addAll(set, Evcs.ChannelId.values());
        Collections.addAll(set, ManagedEvcs.ChannelId.values());
        Collections.addAll(set, MeasuringEvcs.ChannelId.values());
        set.add(ElectricityMeter.ChannelId.ACTIVE_POWER);
        set.add(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);  
        CHANNELS = set.toArray(new io.openems.edge.common.channel.ChannelId[0]);
    }

    private static final Set<OcppInformations> MEASUREMENTS =
            new HashSet<>(Arrays.asList(OcppInformations.values()));

    /* ------------------------------------------------------------------ */
    /* OSGi-Referenzen                                                    */
    /* ------------------------------------------------------------------ */
    private Config config;

    @Reference(policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    private volatile Timedata timedata;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    private volatile EvcsPower evcsPower;

    /* ------------------------------------------------------------------ */
    /* Konstruktor & Lifecycle                                            */
    /* ------------------------------------------------------------------ */
    public EvcsOcppAlfenEveSingleImpl() {
        super(PROFILES, CHANNELS);
    }

    @Activate
    private void activate(ComponentContext ctx, Config cfg) {
        this.config = cfg;
        super.activate(ctx, cfg.id(), cfg.alias(), cfg.enabled());

        _setChargingType(ChargingType.AC);
        _setPhases(Phases.THREE_PHASE);
        _setPowerPrecision(U_NOMINAL_V);
        _setFixedMinimumHardwarePower(getConfiguredMinimumHardwarePower());
        _setFixedMaximumHardwarePower(getConfiguredMaximumHardwarePower());
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
    @Override public MeterType     getMeterType()     { return MeterType.MANAGED_CONSUMPTION_METERED; }
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
}