package io.openems.edge.app.integratedsystem;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestFeneconHome20 {

	private AppManagerTestBundle appManagerTestBundle;

	private SocomecMeter meterApp;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					Apps.feneconHome20(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.socomecMeter(t), //
					Apps.prepareBatteryExtension(t), //
					this.meterApp = Apps.socomecMeter(t) //
			);
		}, null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	public void testCreateHomeFullSettings() throws Exception {
		this.createFullHome();
	}

	@Test
	public void testCreateAndUpdateHomeFullSettings() throws Exception {
		var homeInstance = this.createFullHome();

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", fullSettings()));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(5, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home.20" -> 4;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Meter.Socomec" -> 0;
			case "App.Ess.PrepareBatteryExtension" -> 0;
			default -> throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}
	}

	@Test
	public void testCheckPvs() throws Exception {
		final var homeInstance = this.createFullHome();

		for (int i = 0; i < 4; i++) {
			this.appManagerTestBundle.componentManger.getComponent("charger" + i);
		}

		final var settings = fullSettings();
		settings.addProperty("HAS_PV_3", false);
		settings.addProperty("HAS_PV_4", false);

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", settings));

		for (int i = 0; i < 2; i++) {
			this.appManagerTestBundle.componentManger.getComponent("charger" + i);
		}
		for (int i = 2; i < 4; i++) {
			try {
				this.appManagerTestBundle.componentManger.getComponent("charger" + i);
				assertTrue(false);
			} catch (OpenemsNamedException e) {
				// expected
			}
		}
	}

	@Test
	public void testShadowManagement() throws Exception {
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home.20", "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty("SAFETY_COUNTRY", "GERMANY") //
						.addProperty("FEED_IN_TYPE", FeedInType.DYNAMIC_LIMITATION) //
						.addProperty("MAX_FEED_IN_POWER", 1000) //
						.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
						.addProperty("HAS_EMERGENCY_RESERVE", true) //
						.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
						.addProperty("EMERGENCY_RESERVE_SOC", 15) //
						.addProperty("SHADOW_MANAGEMENT_DISABLED", true) //
						.build()));

		var batteryInverter = this.appManagerTestBundle.componentManger.getComponent("batteryInverter0");
		assertEquals("DISABLE",
				(String) batteryInverter.getComponentContext().getProperties().get("mpptForShadowEnable"));

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(response.instance().instanceId, "alias", JsonUtils.buildJsonObject() //
						.addProperty("SAFETY_COUNTRY", "GERMANY") //
						.addProperty("FEED_IN_TYPE", FeedInType.DYNAMIC_LIMITATION) //
						.addProperty("MAX_FEED_IN_POWER", 1000) //
						.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
						.addProperty("HAS_EMERGENCY_RESERVE", true) //
						.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
						.addProperty("EMERGENCY_RESERVE_SOC", 15) //
						.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
						.build()));
		batteryInverter = this.appManagerTestBundle.componentManger.getComponent("batteryInverter0");
		assertEquals("ENABLE",
				(String) batteryInverter.getComponentContext().getProperties().get("mpptForShadowEnable"));
	}

	@Test
	public void testGetMeterDefaultModbusIdValue() throws Exception {
		this.createFullHome();

		final var modbusIdProperty = Arrays.stream(this.meterApp.getProperties()) //
				.filter(t -> t.name.equals(SocomecMeter.Property.MODBUS_ID.name())) //
				.findFirst().orElseThrow();

		assertEquals("modbus2", modbusIdProperty.getDefaultValue(Language.DEFAULT).map(JsonElement::getAsString).get());
	}

	private final OpenemsAppInstance createFullHome() throws Exception {
		var fullConfig = fullSettings();

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home.20", "key", "alias", fullConfig));

		// make sure every dependency got installed
		assertEquals(5, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home.20" -> 4;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Meter.Socomec" -> 0;
			case "App.Ess.PrepareBatteryExtension" -> 0;
			default -> throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

		var homeInstance = this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.filter(t -> t.appId.equals("App.FENECON.Home.20")).findAny().orElse(null);

		assertNotNull(homeInstance);
		this.appManagerTestBundle.assertNoValidationErrors();
		return homeInstance;
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a {@link FeneconHome20}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("FEED_IN_TYPE", FeedInType.DYNAMIC_LIMITATION) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", true) //
				.addProperty("HAS_PV_1", true) //
				.addProperty("HAS_PV_2", true) //
				.addProperty("HAS_PV_3", true) //
				.addProperty("HAS_PV_4", true) //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

	/**
	 * Gets a {@link JsonObject} with the minimum settings for a
	 * {@link FeneconHome}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject minSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("FEED_IN_TYPE", FeedInType.DYNAMIC_LIMITATION) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", false) //
				.addProperty("HAS_PV_1", false) //
				.addProperty("HAS_PV_2", false) //
				.addProperty("HAS_PV_3", false) //
				.addProperty("HAS_PV_4", false) //
				.addProperty("HAS_EMERGENCY_RESERVE", false) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

}
