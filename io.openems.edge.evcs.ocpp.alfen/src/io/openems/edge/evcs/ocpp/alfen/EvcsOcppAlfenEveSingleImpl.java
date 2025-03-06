package io.openems.edge.evcs.ocpp.alfen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.Alfen.EveSingle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EvcsOcppAlfenEveSingleImpl extends AbstractManagedOcppEvcsComponent
		implements EvcsOcppAlfenEveSingle, Evcs, MeasuringEvcs, ManagedEvcs, ElectricityMeter, OpenemsComponent, EventHandler, ComponentJsonApi {

	// Default value for the hardware limit
	private static final Integer DEFAULT_HARDWARE_LIMIT = 22080;

	// Profiles that a Alfen Eve Single is supporting
	private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE //
	};

	// Values that an Alfen Eve Single is supporting
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

	public EvcsOcppAlfenEveSingleImpl() {
		super(//
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MeasuringEvcs.ChannelId.values(), //
				EvcsOcppAlfenEveSingle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public Set<OcppInformations> getSupportedMeasurements() {
		return MEASUREMENTS;
	}

	@Override
	public String getConfiguredOcppId() {
		return this.config.ocppId();
	}

	@Override
	public Integer getConfiguredConnectorId() {
		return this.config.connectorId();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}
	

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		// TODO implement handling for rotated Phases
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public OcppStandardRequests getStandardRequests() {
		AbstractManagedOcppEvcsComponent evcs = this;

		return new OcppStandardRequests() {

			@Override
			public Request setChargePowerLimit(int chargePower) {
				var phases = evcs.getPhasesAsInt();
				var target = Math.round(chargePower / phases / 230.0);
				var maxCurrent = evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT) / phases / 230;

				target = target > maxCurrent ? maxCurrent : target;

				return new ChangeConfigurationRequest("Station-MaxCurrent", String.valueOf(target));

			}

			@Override
			public Request setChargeCurrentLimit(int chargeCurrent) {

				ChargingProfilePurposeType purpose;
				int chargingProfileId;
				int stackLevel;

				// These parameter values are for testing the optimizer on single connector
				// evcs.
				purpose = ChargingProfilePurposeType.ChargePointMaxProfile;
				chargingProfileId = 1;
				stackLevel = 4;

				ChargingSchedulePeriod[] schedulePeriod = { new ChargingSchedulePeriod(0, (double) chargeCurrent) };
				ChargingSchedule schedule = new ChargingSchedule(ChargingRateUnitType.A, schedulePeriod);
				var profile = new ChargingProfile(chargingProfileId, stackLevel, purpose,
						ChargingProfileKindType.Absolute, schedule);

				return new SetChargingProfileRequest(/* getConfiguredConnectorId() */0, profile);
			}

			@Override
			public Request setTxProfile(int connector, int chargeCurrent) {
				if (connector != EvcsOcppAlfenEveSingleImpl.this.getConfiguredConnectorId()) {
					return null;
				}

				ChargingSchedulePeriod[] schedulePeriod = { new ChargingSchedulePeriod(0, (double) chargeCurrent) };
				ChargingSchedule schedule = new ChargingSchedule(ChargingRateUnitType.A, schedulePeriod);
				ChargingProfilePurposeType purpose = ChargingProfilePurposeType.TxProfile;
				int chargingProfileId = 1;
				int stackLevel = 5;

				var profile = new ChargingProfile(chargingProfileId, stackLevel, purpose,
						ChargingProfileKindType.Absolute, schedule);

				return new SetChargingProfileRequest(EvcsOcppAlfenEveSingleImpl.this.getConfiguredConnectorId(), profile);
			}

			@Override
			public Request setTxDefaultProfile(int connector, int chargeCurrent) {
				ChargingSchedulePeriod[] schedulePeriod = { new ChargingSchedulePeriod(0, (double) chargeCurrent) };
				ChargingSchedule schedule = new ChargingSchedule(ChargingRateUnitType.A, schedulePeriod);
				ChargingProfilePurposeType purpose = ChargingProfilePurposeType.TxDefaultProfile;
				int chargingProfileId = 1;
				int stackLevel = 5;

				var profile = new ChargingProfile(chargingProfileId, stackLevel, purpose,
						ChargingProfileKindType.Absolute, schedule);

				return new SetChargingProfileRequest(connector, profile);
			}

			@Override
			public Request setDisplayText(String text) {
				// Feature not supported
				return null;
			}
		};
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {
		List<Request> requests = new ArrayList<>();

		var setMeterValueSampleInterval = new ChangeConfigurationRequest("MeterValueSampleInterval", "10");
		requests.add(setMeterValueSampleInterval);

		var setMeterValueSampledData = new ChangeConfigurationRequest("MeterValuesSampledData",
				"Current.Import,Voltage,Power.Active.Import,Current.Offered,Energy.Active.Import.Register");
		requests.add(setMeterValueSampledData);

		var setClockAlignedDataInterval = new ChangeConfigurationRequest("ClockAlignedDataInterval", "10");
		requests.add(setClockAlignedDataInterval);

		var setMeterValuesAlignedData = new ChangeConfigurationRequest("MeterValuesAlignedData",
				"Energy.Active.Import.Register,Current.Import,Power.Active.Import,Voltage,Current.Offered");
		requests.add(setMeterValuesAlignedData);

		var setSendStationStatus = new ChangeConfigurationRequest("SendStationStatus", "True");
		requests.add(setSendStationStatus);

		return requests;
	}

	@Override
	public List<Request> getRequiredRequestsDuringConnection() {
		List<Request> requests = new ArrayList<>();

		return requests;
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean returnsSessionEnergy() {
		// TODO: Not tested for now
		return false;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		// TODO check for real world default value
		return 30;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

    @Override
    public void buildJsonApiRoutes(JsonApiBuilder builder) {
        // Handle ApplyChargePowerLimitRequest
        builder.rpc(
            ApplyChargePowerLimitRequest.METHOD,
            call -> {
                ApplyChargePowerLimitRequest request = ApplyChargePowerLimitRequest.from(call.getRequest());
                var chargePowerLimit = request.getChargePowerLimit();

                boolean success = this.applyChargePowerLimit(chargePowerLimit);

                var response = new ApplyChargePowerLimitResponse(request.getId(), success);

                call.setResponse(response);
            }
        );

        // Handle ApplyChargeCurrentLimitRequest
        builder.rpc(
            ApplyChargeCurrentLimitRequest.METHOD,
            call -> {
                ApplyChargeCurrentLimitRequest request = ApplyChargeCurrentLimitRequest.from(call.getRequest());
                var chargeCurrentLimit = request.getChargeCurrentLimit();
                var connectorId = request.getConnectorId();

                CurrentLimitResult success = this.applyChargeCurrentLimit(connectorId, chargeCurrentLimit);

                var response = new ApplyChargeCurrentLimitResponse(request.getId(), success);

                call.setResponse(response);
            }
        );
    }

}
