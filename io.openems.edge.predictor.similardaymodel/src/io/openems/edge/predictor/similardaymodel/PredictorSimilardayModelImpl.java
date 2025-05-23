package io.openems.edge.predictor.similardaymodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.SimilardayModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictorSimilardayModelImpl extends AbstractPredictor implements Predictor, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PredictorSimilardayModelImpl.class);

	public static final int NUM_OF_DAYS_OF_WEEK = 7;
	public static final int NUM_OF_DATA_PER_DAY = 96;

	@Reference
	private Sum sum;

	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public PredictorSimilardayModelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictorSimilardayModel.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
				this.config.channelAddresses(), config.logVerbosity());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.componentManager.getClock()));
		// From now time to Last 4 weeks
		var fromDate = now.minus(this.config.numOfWeeks(), ChronoUnit.WEEKS);

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;

		// Query database
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
			return EMPTY_PREDICTION;
		}

		// Extract data
		List<Integer> result = queryResult.values().stream() //
				.map(SortedMap::values) //
				// extract JsonElement values as flat stream
				.flatMap(Collection::stream) //
				// convert JsonElement to Integer
				.map(v -> {
					if (v.isJsonNull()) {
						return (Integer) null;
					}
					return v.getAsInt();
				})
				// get as Array
				.toList();

		var mainData = this.getSlicedArrayList(result, NUM_OF_DATA_PER_DAY);
		var lastSimilarDays = this.getLastSimilarDays(mainData);
		var nextOneDayPredictions = this.getAveragePrediction(lastSimilarDays);
		return Prediction.from(this.sum, channelAddress, now, nextOneDayPredictions.stream().toArray(Integer[]::new));
	}

	/**
	 * This methods takes a List of integers and returns a 2dimension List of
	 * integers, specific to correct days.
	 *
	 * @param arrlist array list of all data.
	 * @param n       number of data per day.
	 * @return 2dimension array list
	 */
	private List<List<Integer>> getSlicedArrayList(List<Integer> arrlist, int n) {
		List<List<Integer>> twoDimensionalArrayList = new ArrayList<>();
		for (var i = 0; i < arrlist.size(); i = i + n) {
			if ((i + 4 == arrlist.size()) || (i + n - 4 == arrlist.size())) { // winter/summer time clock switch
				break;
			}
			twoDimensionalArrayList.add(arrlist.subList(i, i + n));
		}
		return twoDimensionalArrayList;

	}

	/**
	 * This methods get the average of data based on the indexes.
	 *
	 * @param twoDimensionalArrayList The actual data.
	 * @return Average values of the last four days.
	 */
	private List<Integer> getAveragePrediction(List<List<Integer>> twoDimensionalArrayList) {
		List<Integer> averageList = new ArrayList<>();
		var rows = twoDimensionalArrayList.size();
		var cols = twoDimensionalArrayList.get(0).size();
		for (var i = 0; i < cols; i++) {
			var sumRow = 0;
			for (var j = 0; j < rows; j++) {
				if (twoDimensionalArrayList.get(j).get(i) != null) {
					sumRow += twoDimensionalArrayList.get(j).get(i);
				}
			}
			averageList.add(sumRow / twoDimensionalArrayList.size());
		}
		return averageList;

	}

	/**
	 * Data manipulation, to get the proper indexes.
	 *
	 * @param mainData all data points.
	 * @return proper indexed days.
	 */
	private List<List<Integer>> getLastSimilarDays(List<List<Integer>> mainData) {
		List<Integer> indexes = new ArrayList<>();
		List<List<Integer>> days = new ArrayList<>();
		// get correct index
		for (var i = 0; i < mainData.size(); i++) {
			if (this.isMember(i)) {
				indexes.add(i);
			}
		}
		// get appropriate data for index
		for (Integer i : indexes) {
			days.add(mainData.get(i));
		}

		return days;

	}

	/**
	 * Check if the day belongs to correct day.
	 *
	 * @param arrayIndex the index to check.
	 * @return boolean value to represent is value member of correct day
	 */
	private boolean isMember(int arrayIndex) {
		return arrayIndex % NUM_OF_DAYS_OF_WEEK == 0;

	}

}
