package io.openems.common.timedata;

import static io.openems.common.utils.JsonUtils.toJson;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportCategory;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.ActualEdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfig.Factory;
import io.openems.common.types.EdgeConfig.Factory.Property;

public class XlsxExportUtilTest {

	private EdgeConfig edgeConfig;

	@Before
	public void setup() {
		this.edgeConfig = ActualEdgeConfig.create() //
				.addComponent("charger0", //
						new Component("charger0", "My Charger", "Fenecon.Dess.Charger1",
								// Properties
								ImmutableSortedMap.of(),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter0",
						new Component("meter0", "My CONSUMPTION_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("CONSUMPTION_METERED")),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter1",
						new Component("meter1", "My CONSUMPTION_NOT_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("CONSUMPTION_NOT_METERED")),
								// Channels
								ImmutableSortedMap.of())) //
				.addComponent("meter2", new Component("meter2", "My PRODUCTION Meter", "Meter.Socomec.Threephase",
						// Properties
						ImmutableSortedMap.of("type", toJson("PRODUCTION")),
						// Channels
						ImmutableSortedMap.of())) //
				.addComponent("meter3",
						new Component("meter3", "My MANAGED_CONSUMPTION_METERED Meter", "Meter.Socomec.Threephase",
								// Properties
								ImmutableSortedMap.of("type", toJson("MANAGED_CONSUMPTION_METERED")),
								// Channels
								ImmutableSortedMap.of())) //

				.addFactory("Meter.Socomec.Threephase",
						new Factory("Meter.Socomec.Threephase", "My Name", "My Description", //
								new Property[] {}, //
								// Natures
								new String[] { "io.openems.edge.meter.api.ElectricityMeter" })) //
				.addFactory("Fenecon.Dess.Charger1", new Factory("Fenecon.Dess.Charger1", "My Name", "My Description", //
						new Property[] {}, //
						// Natures
						new String[] { "io.openems.edge.ess.dccharger.api.EssDcCharger" })) //
				.buildEdgeConfig();

	}

	@Test
	public void testGetConsumptionData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var consumptions = result.data().get(XlsxExportCategory.CONSUMPTION);

		{
			var meter = consumptions.get(0);
			assertEquals("My CONSUMPTION_METERED Meter", meter.alias());
			assertEquals("meter0/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(1);
			assertEquals("My CONSUMPTION_NOT_METERED Meter", meter.alias());
			assertEquals("meter1/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}
		{
			var meter = consumptions.get(2);
			assertEquals("My MANAGED_CONSUMPTION_METERED Meter", meter.alias());
			assertEquals("meter3/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

	}

	@Test
	public void testGetProductionData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var productions = result.data().get(XlsxExportCategory.PRODUCTION);
		assertEquals(2, productions.size());

		{
			var meter = productions.get(0);
			assertEquals("My Charger", meter.alias());
			assertEquals("charger0/ActualPower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

		{
			var meter = productions.get(1);
			assertEquals("My PRODUCTION Meter", meter.alias());
			assertEquals("meter2/ActivePower", meter.channel().toString());
			assertEquals(HistoricTimedataSaveType.POWER, meter.type());
		}

		var touts = result.data().get(XlsxExportCategory.TIME_OF_USE_TARIFF);
		assertEquals(0, touts.size());
	}

	@Test
	public void testGetToutsData() throws OpenemsNamedException {
		final var result = XlsxExportUtil.getDetailData(this.edgeConfig);

		var touts = result.data().get(XlsxExportCategory.TIME_OF_USE_TARIFF);
		assertEquals(0, touts.size());
	}
}
