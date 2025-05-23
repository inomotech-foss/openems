package io.openems.edge.common.type;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;

/**
 * Handles implicit conversions between {@link OpenemsType}s.
 */
public class TypeUtils {

	/**
	 * Converts and casts a Object to a given type.
	 *
	 * @param <T>   the Type for implicit casting of the result
	 * @param type  the type as {@link OpenemsType}
	 * @param value the value as {@link Object}
	 * @return the converted and casted value
	 * @throws IllegalArgumentException on error
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAsType(OpenemsType type, Object value) throws IllegalArgumentException {
		// Handle null value
		if (value == null) {
			return null;
		}
		// Extract Value containers
		if (value instanceof Value<?> v) {
			return getAsType(type, v.get());
		}
		// Extract Optionals
		if (value instanceof Optional<?> v) {
			return getAsType(type, v.orElse(null));
		}
		// Extract OptionsEnum
		if (value instanceof OptionsEnum oe) {
			return getAsType(type, oe.getValue());
		}
		// Extract Enum (lower priority than OptionsEnum)
		if (value instanceof Enum<?> e) {
			return getAsType(type, e.ordinal());
		}
		// Extract value from Array
		if (type != OpenemsType.STRING && value != null && value.getClass().isArray()) {
			if (Array.getLength(value) == 1) {
				return getAsType(type, Array.get(value, 0));
			}
			return null;
		}

		// Handle special floating point numbers
		if (value instanceof Float f && (f.isInfinite() || f.isNaN())) {
			return null;
		}
		if (value instanceof Double d && (d.isInfinite() || d.isNaN())) {
			return null;
		}

		return (T) switch (type) {
		case BOOLEAN //
			-> switch (value) {
			case Boolean b -> b;
			case Short s -> s == 0 ? Boolean.FALSE : Boolean.TRUE;
			case Integer i -> i == 0 ? Boolean.FALSE : Boolean.TRUE;
			case Long l -> l == 0 ? Boolean.FALSE : Boolean.TRUE;
			case Float f -> f == 0 ? Boolean.FALSE : Boolean.TRUE;
			case Double d -> d == 0 ? Boolean.FALSE : Boolean.TRUE;
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				} else if (s.equalsIgnoreCase("false")) {
					yield Boolean.FALSE;
				} else if (s.equalsIgnoreCase("true")) {
					yield Boolean.TRUE;
				} else {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Boolean.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case SHORT //
			-> switch (value) {
			case Boolean b -> Short.valueOf(b ? (short) 1 : (short) 0);
			case Short s -> s;
			case Integer i -> {
				var intValue = i.intValue();
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					yield Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Integer [" + value + "] is not fitting in Short range.");
				}
			}
			case Long l -> {
				var longValue = l.longValue();
				if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
					yield Short.valueOf((short) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Long [" + value + "] is not fitting in Short range.");
				}
			}
			case Float f -> {
				var intValue = Math.round(f.floatValue());
				if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					yield Short.valueOf((short) intValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Short range.");
				}
			}
			case Double d -> {
				var longValue = Math.round(d.doubleValue());
				if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
					yield Short.valueOf((short) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Short range.");
				}
			}
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				}
				try {
					yield Short.valueOf(Short.parseShort(s));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Short.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case INTEGER //
			-> switch (value) {
			case Boolean b -> Integer.valueOf(b ? 1 : 0);
			case Short s -> Integer.valueOf(s);
			case Integer i -> i;
			case Long l -> {
				var longValue = l.longValue();
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					yield Integer.valueOf((int) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Long [" + value + "] is not fitting in Integer range.");
				}
			}
			case Float f -> {
				var floatValue = f.floatValue();
				if (floatValue >= Integer.MIN_VALUE && floatValue <= Integer.MAX_VALUE) {
					yield Integer.valueOf((int) floatValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Integer range.");
				}
			}
			case Double d -> {
				var longValue = Math.round(d.doubleValue());
				if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
					yield Integer.valueOf((int) longValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Integer range.");
				}
			}
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				}
				try {
					yield Integer.valueOf(Integer.parseInt(s));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Integer.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case LONG //
			-> switch (value) {
			case Boolean b -> Long.valueOf(b ? 1L : 0L);
			case Short s -> (Long) s.longValue();
			case Integer i -> (Long) i.longValue();
			case Long l -> l;
			case Float f -> {
				var floatValue = f.floatValue();
				if (floatValue >= Long.MIN_VALUE && floatValue <= Long.MAX_VALUE) {
					yield Long.valueOf((long) floatValue);
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Float [" + value + "] is not fitting in Long range.");
				}
			}
			case Double d -> {
				var doubleValue = d.doubleValue();
				if (doubleValue >= Long.MIN_VALUE && doubleValue <= Long.MAX_VALUE) {
					yield (Long) Math.round(d.doubleValue());
				} else {
					throw new IllegalArgumentException(
							"Cannot convert. Double [" + value + "] is not fitting in Long range.");
				}
			}
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				}
				try {
					yield Long.valueOf(Long.parseLong(s));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Long.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case FLOAT //
			-> switch (value) {
			case Boolean b -> Float.valueOf(b ? 1f : 0f);
			case Short s -> (Float) s.floatValue();
			case Integer i -> (Float) i.floatValue();
			case Long l -> (Float) l.floatValue();
			case Float f -> f;
			case Double d ->
				// Returns the value of this Double as a float after a narrowing primitive
				// conversion.
				Float.valueOf(d.floatValue());
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				}
				try {
					yield Float.valueOf(Float.parseFloat(s));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Float.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case DOUBLE //
			-> switch (value) {
			case Boolean b -> Double.valueOf(b ? 1L : 0L);
			case Short s -> Double.valueOf(s);
			case Integer i -> Double.valueOf(i);
			case Long l -> Double.valueOf(l);
			case Float f -> Double.valueOf(f);
			case Double d -> d;
			case String s -> {
				if (s.isEmpty()) {
					yield null;
				}
				try {
					yield Double.valueOf(s);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Cannot convert String [" + s + "] to Double.");
				}
			}
			default -> throw converterIsNotImplemented(type, value);
			};

		case STRING -> {
			if (value instanceof Object[] os) {
				yield Arrays.deepToString(os);

			} else if (value.getClass().isArray()) {
				yield switch (value) {
				case boolean[] bs -> Arrays.toString(bs);
				case byte[] bs -> Arrays.toString(bs);
				case char[] cs -> Arrays.toString(cs);
				case double[] ds -> Arrays.toString(ds);
				case float[] fs -> Arrays.toString(fs);
				case int[] is -> Arrays.toString(is);
				case long[] ls -> Arrays.toString(ls);
				case short[] ss -> Arrays.toString(ss);
				default -> value.toString();
				};

			} else {
				yield value.toString();
			}
		}
		};
	}

	private static IllegalArgumentException converterIsNotImplemented(OpenemsType type, Object value) {
		return new IllegalArgumentException("Converter for value [" + value + "] of type [" + value.getClass()
				+ "] to type [" + type + "] is not implemented.");
	}

	/**
	 * Gets the value of the given type as {@link JsonElement}.
	 *
	 * @param type          the type as {@link OpenemsType}
	 * @param originalValue the value
	 * @return the converted value
	 */
	public static JsonElement getAsJson(OpenemsType type, Object originalValue) {
		if (originalValue == null) {
			return JsonNull.INSTANCE;
		}
		var value = TypeUtils.getAsType(type, originalValue);
		return switch (type) {
		case BOOLEAN -> new JsonPrimitive((Boolean) value ? 1 : 0);
		case SHORT -> new JsonPrimitive((Short) value);
		case INTEGER -> new JsonPrimitive((Integer) value);
		case LONG -> new JsonPrimitive((Long) value);
		case FLOAT -> new JsonPrimitive((Float) value);
		case DOUBLE -> new JsonPrimitive((Double) value);
		case STRING -> new JsonPrimitive((String) value);
		};
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Integer} values
	 * @return the sum
	 */
	public static Integer sumInt(List<Integer> values) {
		return sum(values.toArray(Integer[]::new));
	}

	/**
	 * Safely add Longs. If one of them is null it is considered '0'. If all of them
	 * are null, 'null' is returned.
	 *
	 * @param values the {@link Long} values
	 * @return the sum
	 */
	public static Long sumLong(List<Long> values) {
		return sum(values.toArray(Long[]::new));
	}

	/**
	 * Safely add Integers. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Integer} values
	 * @return the sum
	 */
	public static Integer sum(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely add Floats. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 *
	 * @param values the {@link Float} values
	 * @return the sum
	 */
	public static Float sum(Float... values) {
		Float result = null;
		for (Float value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely add Longs. If one of them is null it is considered '0'. If all of them
	 * are null, 'null' is returned.
	 *
	 * @param values the {@link Long} values
	 * @return the sum
	 */
	public static Long sum(Long... values) {
		Long result = null;
		for (Long value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely add Doubles. If one of them is null it is considered '0'. If all of
	 * them are null, 'null' is returned.
	 * 
	 * @param values the {@link Double} values
	 * @return the sum, possibly null
	 */
	public static Double sum(Double... values) {
		Double result = null;
		for (var value : values) {
			if (value == null) {
				continue;
			}
			if (result == null) {
				result = value;
			} else {
				result += value;
			}
		}
		return result;
	}

	/**
	 * Safely subtract Integers.
	 *
	 * <ul>
	 * <li>if minuend is null -&gt; result is null
	 * <li>if subtrahend is null -&gt; result is minuend
	 * <li>if both are null -&gt; result is null
	 * </ul>
	 *
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 */
	public static Integer subtract(Integer minuend, Integer subtrahend) {
		if (minuend == null) {
			return null;
		}
		if (subtrahend == null) {
			return minuend;
		}
		return minuend - subtrahend;
	}

	/**
	 * Safely subtract Longs.
	 *
	 * <ul>
	 * <li>if minuend is null -&gt; result is null
	 * <li>if subtrahend is null -&gt; result is minuend
	 * <li>if both are null -&gt; result is null
	 * </ul>
	 *
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 */
	public static Long subtract(Long minuend, Long subtrahend) {
		if (minuend == null) {
			return null;
		}
		if (subtrahend == null) {
			return minuend;
		}
		return minuend - subtrahend;
	}

	/**
	 * Safely subtract Doubles.
	 *
	 * <ul>
	 * <li>if minuend is null -&gt; result is null
	 * <li>if subtrahend is null -&gt; result is minuend
	 * <li>if both are null -&gt; result is null
	 * </ul>
	 *
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 */
	public static Double subtract(Double minuend, Double subtrahend) {
		if (minuend == null) {
			return null;
		}
		if (subtrahend == null) {
			return minuend;
		}
		return minuend - subtrahend;
	}

	/**
	 * Safely multiply Integers.
	 *
	 * @param firstFactor    first factor of the multiplication
	 * @param furtherFactors further factors of the multiplication
	 * @return the result, possibly null if the first factor is null
	 */
	public static Integer multiply(Integer firstFactor, Integer... furtherFactors) {
		if (firstFactor == null) {
			return null;
		}
		int result = firstFactor;
		for (Integer factor : furtherFactors) {
			if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely multiply Floats.
	 *
	 * @param factors the factors of the multiplication
	 * @return the result, possibly null if all factors are null
	 */
	public static Float multiply(Float... factors) {
		Float result = null;
		for (var factor : factors) {
			if (result == null) {
				result = factor;
			} else if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely multiply Doubles.
	 *
	 * @param factors the factors of the multiplication
	 * @return the result, possibly null if all factors are null
	 */
	public static Double multiply(Double... factors) {
		Double result = null;
		for (Double factor : factors) {
			if (result == null) {
				result = factor;
			} else if (factor != null) {
				result *= factor;
			}
		}
		return result;
	}

	/**
	 * Safely divides Integers.
	 *
	 * <ul>
	 * <li>if dividend is null -&gt; result is null
	 * </ul>
	 *
	 * @param dividend the dividend of the division
	 * @param divisor  the divisor of the division
	 * @return the result, possibly null
	 */
	public static Integer divide(Integer dividend, int divisor) {
		if (dividend == null) {
			return null;
		}
		return dividend / divisor;
	}

	/**
	 * Safely divides Longs.
	 *
	 * <ul>
	 * <li>if dividend is null -&gt; result is null
	 * </ul>
	 *
	 * @param dividend the dividend of the division
	 * @param divisor  the divisor of the division
	 * @return the result, possibly null
	 */
	public static Long divide(Long dividend, long divisor) {
		if (dividend == null) {
			return null;
		}
		return dividend / divisor;
	}

	/**
	 * Safely finds the max value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the max value; or null if all values are null
	 */
	public static Integer max(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.max(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the max value of all values.
	 *
	 * @param values the {@link Float} values
	 * @return the max value; or null if all values are null
	 */
	public static Float max(Float... values) {
		Float result = null;
		for (var value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.max(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the min value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the min value; or null if all values are null
	 */
	public static Integer min(Integer... values) {
		Integer result = null;
		for (Integer value : values) {
			if (result != null && value != null) {
				result = Math.min(result, value);
			} else if (value != null) {
				result = value;
			}
		}
		return result;
	}

	/**
	 * Safely finds the min value of all values.
	 *
	 * @param values the {@link Double} values
	 * @return the min value; or null if all values are null
	 */
	public static Double min(Double... values) {
		Double result = null;
		for (Double value : values) {
			if (value != null) {
				if (result == null) {
					result = value;
				} else {
					result = Math.min(result, value);
				}
			}
		}
		return result;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the average value; or null if all values are null
	 */
	public static Float average(Integer... values) {
		var count = 0;
		var sum = 0.f;
		for (Integer value : values) {
			if (value != null) {
				count++;
				sum += value;
			}
		}
		if (count == 0) {
			return null;
		}
		return sum / count;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the double values
	 * @return the average value; or Double.NaN if all values are invalid.
	 */
	public static double average(double... values) {
		var count = 0;
		var sum = 0.;
		for (double value : values) {
			if (Double.isNaN(value)) {
				continue;
			}
			count++;
			sum += value;
		}
		if (count == 0) {
			return Double.NaN;
		}
		return sum / count;
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the average value; or null if all values are null
	 */
	public static Integer averageInt(Integer... values) {
		var count = 0;
		float sum = 0;
		for (Integer value : values) {
			if (value != null) {
				count++;
				sum += value;
			}
		}
		if (count == 0) {
			return null;
		}
		return Math.round(sum / count);
	}

	/**
	 * Safely finds the average value of all values.
	 *
	 * @param values the {@link Integer} values
	 * @return the average value; or null if all values are null
	 */
	public static Integer averageInt(List<Integer> values) {
		return averageInt(values.toArray(Integer[]::new));
	}

	/**
	 * Safely finds the average value of all values and rounds the result to an
	 * Integer using {@link Math#round(float)}.
	 *
	 * @param values the {@link Integer} values
	 * @return the rounded average value; or null if all values are null
	 */
	public static Integer averageRounded(Integer... values) {
		var result = average(values);
		if (result == null) {
			return null;
		}
		return Math.round(result);
	}

	/**
	 * Throws an descriptive exception if the object is null.
	 *
	 * @param description text that is added to the exception
	 * @param objects     the objects
	 * @throws IllegalArgumentException if any object is null
	 */
	public static void assertNull(String description, Object... objects) throws IllegalArgumentException {
		for (Object object : objects) {
			if (object == null) {
				throw new IllegalArgumentException(description + ": value is null!");
			}
		}
	}

	/**
	 * Safely convert from {@link Integer} to {@link Double}.
	 *
	 * @param value the Integer value, possibly null
	 * @return the Double value, possibly null
	 */
	public static Double toDouble(Integer value) {
		if (value == null) {
			return null;
		}
		return Double.valueOf(value);
	}

	/**
	 * Safely convert from {@link Float} to {@link Double}.
	 *
	 * @param value the Float value, possibly null
	 * @return the Double value, possibly null
	 */
	public static Double toDouble(Float value) {
		if (value == null) {
			return null;
		}
		return Double.valueOf(value);
	}

	/**
	 * Returns the 'alternativeValue' if the 'nullableValue' is null.
	 *
	 * @param <T>              the Type for implicit casting
	 * @param nullableValue    the value, can be null
	 * @param alternativeValue the alternative value
	 * @return either the value (not null), alternatively the 'orElse' value
	 */
	public static <T> T orElse(T nullableValue, T alternativeValue) {
		if (nullableValue != null) {
			return nullableValue;
		}
		return alternativeValue;
	}

	/**
	 * Fits a value within a lower and upper boundary.
	 *
	 * @param lowLimit  the int lower boundary
	 * @param highLimit the int upper boundary
	 * @param value     the int actual value
	 * @return the adjusted int value
	 */
	public static int fitWithin(int lowLimit, int highLimit, int value) {
		return Math.max(lowLimit, //
				Math.min(highLimit, value));
	}

	/**
	 * Fits a value within a lower and upper boundary.
	 *
	 * @param lowLimit  the long lower boundary
	 * @param highLimit the long upper boundary
	 * @param value     the long actual value
	 * @return the adjusted long value
	 */
	public static long fitWithin(long lowLimit, long highLimit, long value) {
		return Math.max(lowLimit, //
				Math.min(highLimit, value));
	}

	/**
	 * Fits a value within a lower and upper boundary.
	 *
	 * @param lowLimit  the double lower boundary
	 * @param highLimit the double upper boundary
	 * @param value     the double actual value
	 * @return the adjusted double value
	 */
	public static double fitWithin(double lowLimit, double highLimit, double value) {
		return Math.max(lowLimit, //
				Math.min(highLimit, value));
	}

	/**
	 * Fits a value within a lower and upper boundary.
	 *
	 * @param lowLimit  the float lower boundary
	 * @param highLimit the float upper boundary
	 * @param value     the float actual value
	 * @return the adjusted float value
	 */
	public static float fitWithin(float lowLimit, float highLimit, float value) {
		return Math.max(lowLimit, //
				Math.min(highLimit, value));
	}

	/**
	 * Safely returns the absolute value of an Integer value.
	 *
	 * @param value the Integer value, possibly null
	 * @return the absolute value, possibly null
	 */
	public static Integer abs(Integer value) {
		if (value == null) {
			return null;
		}
		return Math.abs(value);
	}
}
