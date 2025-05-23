package io.openems.common.jscalendar;

import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.DAILY;
import static io.openems.common.jscalendar.JSCalendar.RecurrenceFrequency.WEEKLY;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.prettyToString;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar.Tasks.OneTask;
import io.openems.common.utils.JsonUtils;

//CHECKSTYLE:OFF
public class JSCalendarTest {
	// CHECKSTYLE:ON

	@Test
	public void testDaily() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<JsonObject>create() //
				.setStart("07:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(DAILY)) //
				.build();

		var next = sut.getNextOccurence(ZonedDateTime.now(clock));
		assertEquals("2020-01-01T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-02T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-03T07:00Z", next.toString());
	}

	@Test
	public void testDailyParse() throws OpenemsNamedException {
		var sut = JSCalendar.Tasks.fromStringOrEmpty("""
				[
				   {
				      "@type":"Task",
				      "start":"19:00:00",
				      "duration":"PT12H",
				      "recurrenceRules":[
				         {
				            "frequency":"daily"
				         }
				      ]
				   }
				]""", j -> j);
		assertEquals(1, sut.size());
	}

	@Test
	public void testWeekday() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<JsonObject>create() //
				.setStart("07:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.addByDay(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
				.setPayload(buildJsonObject() //
						.addProperty("sessionEnergy", 10000) //
						.build()) //
				.build();

		assertEquals("""
				{
				  "@type": "Task",
				  "start": "07:00:00",
				  "recurrenceRules": [
				    {
				      "frequency": "weekly",
				      "byDay": [
				        "mo",
				        "tu",
				        "we",
				        "th",
				        "fr"
				      ]
				    }
				  ],
				  "openems.io:payload": {
				    "sessionEnergy": 10000
				  }
				}""", prettyToString(sut.toJson(identity())));

		var next = sut.getNextOccurence(ZonedDateTime.now(clock));
		assertEquals("2020-01-01T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-02T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-03T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-06T07:00Z", next.toString()); // next week
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-07T07:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2020-01-08T07:00Z", next.toString());
		next = sut.getNextOccurence(next);
		assertEquals("2020-01-08T07:00Z", next.toString()); // same

		// Parse JSON
		var fromJson = JSCalendar.Tasks.fromJson(buildJsonArray() //
				.add(sut.toJson(identity())) //
				.build(), j -> j);
		assertEquals(sut.toJson(identity()), fromJson.get(0).toJson(identity()));
	}

	@Test
	public void testWeekend() throws OpenemsNamedException {
		var clock = createDummyClock();
		var sut = JSCalendar.Task.<JsonObject>create() //
				.setStart("2024-06-17T00:00:00") //
				.addRecurrenceRule(b -> b //
						.setFrequency(WEEKLY) //
						.addByDay(SATURDAY, SUNDAY)) //
				.setPayload(buildJsonObject() //
						.addProperty("sessionEnergy", 10001) //
						.build()) //
				.build();
		assertEquals("""
				{
				  "@type": "Task",
				  "start": "2024-06-17T00:00:00",
				  "recurrenceRules": [
				    {
				      "frequency": "weekly",
				      "byDay": [
				        "sa",
				        "su"
				      ]
				    }
				  ],
				  "openems.io:payload": {
				    "sessionEnergy": 10001
				  }
				}""", prettyToString(sut.toJson(identity())));

		var next = sut.getNextOccurence(ZonedDateTime.now(clock));
		assertEquals("2024-06-22T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-23T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-29T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-06-30T00:00Z", next.toString());
		next = sut.getNextOccurence(next.plusSeconds(1));
		assertEquals("2024-07-06T00:00Z", next.toString());

		// Parse JSON
		var fromJson = JSCalendar.Task.fromJson(sut.toJson(identity()), identity());
		assertEquals(sut.toJson(identity()), fromJson.toJson(identity()));
	}

	@Test
	public void testGetOccurencesBetween() throws OpenemsNamedException {
		var smartConfig = ImmutableList.of(//
				JSCalendar.Task.<JsonObject>create() //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 10000) //
								.build()) //
						.build(), //
				JSCalendar.Task.<JsonObject>create() //
						.setStart("1970-01-01T07:30:00") //
						.addRecurrenceRule(b -> b //
								.setFrequency(WEEKLY) //
								.addByDay(MONDAY)) //
						.setPayload(buildJsonObject() //
								.addProperty("sessionEnergyMinimum", 60000) //
								.build()) //
						.build());

		var firstTime = ZonedDateTime.parse("2025-03-23T22:45+01:00[Europe/Berlin]");
		var lastTime = ZonedDateTime.parse("2025-04-01T23:00+01:00[Europe/Berlin]");
		var ots = JSCalendar.Tasks.getOccurencesBetween(smartConfig, firstTime, lastTime);

		testOneTask(ots.get(0), "2025-03-24T07:30+01:00[Europe/Berlin]", 60000);
		testOneTask(ots.get(1), "2025-03-25T07:30+01:00[Europe/Berlin]", 10000);
		testOneTask(ots.get(2), "2025-03-26T07:30+01:00[Europe/Berlin]", 10000);
		testOneTask(ots.get(3), "2025-03-27T07:30+01:00[Europe/Berlin]", 10000);
		testOneTask(ots.get(4), "2025-03-28T07:30+01:00[Europe/Berlin]", 10000);
		testOneTask(ots.get(5), "2025-03-31T07:30+02:00[Europe/Berlin]", 60000);
		testOneTask(ots.get(6), "2025-04-01T07:30+02:00[Europe/Berlin]", 10000);
	}

	private static void testOneTask(OneTask<JsonObject> ot, String start, int sessionEnergyMinimum)
			throws OpenemsNamedException {
		assertEquals(start, ot.start().toString());
		assertEquals(sessionEnergyMinimum, JsonUtils.getAsInt(ot.payload(), "sessionEnergyMinimum"));
	}
}
