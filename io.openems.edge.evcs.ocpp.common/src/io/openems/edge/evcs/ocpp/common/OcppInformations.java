package io.openems.edge.evcs.ocpp.common;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.SocEvcs;

public enum OcppInformations {

	/**
	 * Instantaneous current flow from EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_EXPORT("Current.Export", MeasuringEvcs.ChannelId.CURRENT_TO_GRID),

	/**
	 * Instantaneous current flow to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_IMPORT("Current.Import", MeasuringEvcs.ChannelId.CURRENT_TO_EV),

	/**
	 * Instantaneous current flow on L1 to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_IMPORT_L1("Current.Import.L1", MeasuringEvcs.ChannelId.CURRENT_TO_EV_L1),

	/**
	 * Instantaneous current flow on L2 to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_IMPORT_L2("Current.Import.L2", MeasuringEvcs.ChannelId.CURRENT_TO_EV_L2),

	/**
	 * Instantaneous current flow on L3 to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_IMPORT_L3("Current.Import.L3", MeasuringEvcs.ChannelId.CURRENT_TO_EV_L3),

	/**
	 * Maximum current offered to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_OFFERED("Current.Offered", MeasuringEvcs.ChannelId.CURRENT_OFFERED),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy exported (to
	 * the grid).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_REGISTER("Energy.Active.Export.Register",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_GRID),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy imported
	 * (from the grid supply).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER("Energy.Active.Import.Register",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy imported
	 * (from the grid supply on L1).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER_L1("Energy.Active.Import.Register.L1",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV_L1),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy imported
	 * (from the grid supply on L2).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER_L2("Energy.Active.Import.Register.L2",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV_L2),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy imported
	 * (from the grid supply on L3).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER_L3("Energy.Active.Import.Register.L3",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV_L3),

	/**
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy exported (to the
	 * grid).
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_REGISTER("Energy.Reactive.Export.Register",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_GRID),

	/**
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy imported (from the
	 * grid supply).
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_REGISTER("Energy.Reactive.Import.Register",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_EV),

	/**
	 * Absolute amount of "active electrical energy" (Wh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_INTERVAL("Energy.Active.Export.Interval",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_GRID_INTERVAL),

	/**
	 * Absolute amount of "active electrical energy" (Wh) imported (from the grid
	 * supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_INTERVAL("Energy.Active.Import.Interval",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV_INTERVAL),

	/**
	 * Absolute amount of "reactive electrical energy" (VARh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_INTERVAL("Energy.Reactive.Export.Interval",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_GRID_INTERVAL),

	/**
	 * Absolute amount of "reactive electrical energy" (VARh) imported (from the
	 * grid supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_INTERVAL("Energy.Reactive.Import.Interval",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_EV_INTERVAL),

	/**
	 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a
	 * UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with
	 * measurand: Frequency is Hertz.
	 */
	CORE_METER_VALUES_FREQUENCY("Frequency", MeasuringEvcs.ChannelId.FREQUENCY),

	/**
	 * Instantaneous active power exported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_EXPORT("Power.Active.Export", MeasuringEvcs.ChannelId.POWER_ACTIVE_TO_GRID),

	/**
	 * Instantaneous active power imported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_IMPORT("Power.Active.Import", Evcs.ChannelId.CHARGE_POWER),

	/**
	 * Instantaneous active power on L1 imported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_IMPORT_L1("Power.Active.Import.L1",
			Evcs.ChannelId.CHARGE_POWER_L1),

	/**
	 * Instantaneous active power on L2 imported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_IMPORT_L2("Power.Active.Import.L2", Evcs.ChannelId.CHARGE_POWER_L2),

	/**
	 * Instantaneous active power on L3 imported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_IMPORT_L3("Power.Active.Import.L3", Evcs.ChannelId.CHARGE_POWER_L3),

	/**
	 * Instantaneous power factor of total energy flow.
	 */
	CORE_METER_VALUES_POWER_FACTOR("Power.Factor", MeasuringEvcs.ChannelId.POWER_FACTOR),

	/**
	 * Maximum power offered to EV.
	 */
	CORE_METER_VALUES_POWER_OFFERED("Power.Offered", MeasuringEvcs.ChannelId.POWER_OFFERED),

	// TODO: should be combined to REACTIVE_POWER in ElectricityMeter
	/**
	 * Instantaneous reactive power exported by EV. (var)
	 */
	CORE_METER_VALUES_POWER_REACTIVE_EXPORT("Power.Reactive.Export", MeasuringEvcs.ChannelId.POWER_REACTIVE_TO_GRID),

	/**
	 * Instantaneous reactive power imported by EV. (var)
	 */
	CORE_METER_VALUES_POWER_REACTIVE_IMPORT("Power.Reactive.Import", MeasuringEvcs.ChannelId.POWER_REACTIVE_TO_EV),

	/**
	 * Fan speed in RPM.
	 */
	CORE_METER_VALUES_RPM("RPM", MeasuringEvcs.ChannelId.RPM),

	/**
	 * State of charge of charging vehicle in percentage.
	 */
	CORE_METER_VALUES_SOC("SoC", SocEvcs.ChannelId.SOC),

	/**
	 * Temperature reading inside Charge Point.
	 */
	CORE_METER_VALUES_TEMPERATURE("Temperature", MeasuringEvcs.ChannelId.TEMPERATURE),

	/**
	 * Instantaneous AC RMS supply voltage.
	 */
	CORE_METER_VALUES_VOLTAGE("Voltage", MeasuringEvcs.ChannelId.VOLTAGE),

	/**
	 * Instantaneous AC RMS supply voltage L1-N.
	 */
	CORE_METER_VALUES_VOLTAGE_L1_N("Voltage.L1-N", MeasuringEvcs.ChannelId.VOLTAGE_L1_N),

	/**
	 * Instantaneous AC RMS supply voltage L2-N.
	 */
	CORE_METER_VALUES_VOLTAGE_L2_N("Voltage.L2-N", MeasuringEvcs.ChannelId.VOLTAGE_L2_N),

	/**
	 * Instantaneous AC RMS supply voltage L3-N.
	 */
	CORE_METER_VALUES_VOLTAGE_L3_N("Voltage.L3-N", MeasuringEvcs.ChannelId.VOLTAGE_L3_N),

	/**
	 * Instantaneous AC RMS supply voltage L1-L2.
	 */
	CORE_METER_VALUES_VOLTAGE_L1_L2("Voltage.L1-L2", MeasuringEvcs.ChannelId.VOLTAGE_L1_L2),

	/**
	 * Instantaneous AC RMS supply voltage L2-L3.
	 */
	CORE_METER_VALUES_VOLTAGE_L2_L3("Voltage.L2-L3", MeasuringEvcs.ChannelId.VOLTAGE_L2_L3),

	/**
	 * Instantaneous AC RMS supply voltage L3-L1.
	 */
	CORE_METER_VALUES_VOLTAGE_L3_L1("Voltage.L3-L1", MeasuringEvcs.ChannelId.VOLTAGE_L3_L1);

	private final String ocppValue;
	private final ChannelId channelId;

	private OcppInformations(String ocppValue, ChannelId channelId) {
		this.ocppValue = ocppValue;
		this.channelId = channelId;
	}

	public String getOcppValue() {
		return this.ocppValue;
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}
}
