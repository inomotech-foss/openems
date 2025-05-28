package io.openems.edge.evcs.api;

import java.time.Instant;

/**
 * Set the ChargeState from increasing and reducing to charging after minimum
 * time until the charge limit is taken.
 */
public class ChargeStateHandler {

	private final ManagedEvcs parent;
	private Instant lastChargeStateTime = Instant.MIN;
	private ChargeState chargeState = ChargeState.UNDEFINED;

	public ChargeStateHandler(ManagedEvcs parent) {
		this.parent = parent;
	}

	/**
	 * Combined detection of an active charging process:
	 * <ol>
	 * <li>At least one phase current > 450 mA</li>
	 * <li>Fallback: Active power ≥ {@code POWER_PRECISION} of EVCS (typically
	 * 230 W)</li>
	 * </ol>
	 */
	public boolean isChargingDetected() {
		int i1 = parent.getCurrentL1().orElse(0);
		int i2 = parent.getCurrentL2().orElse(0);
		int i3 = parent.getCurrentL3().orElse(0);

		// 1) Current criterion - actual charging only above > 450 mA per phase
		
		int x = Evcs.MIN_EVCS_ACTIVITY_CURRENT;
		if (i1 > x || i2 > x || i3 > x) {
			return true;
		}

		// 2) Fallback - active power threshold = POWER_PRECISION
		int p = parent.getActivePower().orElse(0);
		int precision = parent.getPowerPrecision().orElse(Evcs.DEFAULT_POWER_PRECISION);
		
		return p >= precision;
	}

	/**
	 * Apply a new ChargeState.
	 * 
	 * <p>
	 * Set the ChargeState, if there is no pause active (for increasing and
	 * reducing). The Pause will be ignored and reset, if the given ChargeState is
	 * changing from INCREASING to REDUCING.
	 *
	 * @param reqChargeState ChargeState
	 * @return true when the requiredChargeState is taken
	 */
	public boolean applyNewChargeState(ChargeState reqChargeState) {

		// Ignore and reset the pause if there is the need to reduce (Should reduce
		// immediately)
		if (this.chargeState == ChargeState.INCREASING && reqChargeState == ChargeState.DECREASING) {
			this.setChargeState(reqChargeState);
			this.startPause();
			return true;
		}

		// Set the charge state - Start the pause when the power is increasing or
		// reducing
		if (!this.isPauseActive()) {
			switch (reqChargeState) {
			case INCREASING:
			case DECREASING:
				this.startPause();
				break;
			default:
				break;
			}
			this.setChargeState(reqChargeState);
			return true;
		} else {
			// Pause active
			return reqChargeState == this.chargeState;
		}
	}

	/**
	 * Start pause.
	 */
	private void startPause() {
		this.lastChargeStateTime = Instant.now();
	}

	/**
	 * Is pause active.
	 *
	 * @return boolean
	 */
	private boolean isPauseActive() {
		// Pause not started
		if (Instant.MIN.equals(this.lastChargeStateTime)) {
			return false;
		}

		// Pause active for a minimum time till the charging limit has been taken
		long timeTillLimitTaken = this.parent.getMinimumTimeTillChargingLimitTaken();
		if (Instant.now().minusSeconds(timeTillLimitTaken).isAfter(this.lastChargeStateTime)) {
			this.lastChargeStateTime = Instant.MIN;
			return false;
		}
		return true;
	}

	/**
	 * Get ChargeState.
	 * 
	 * @return current ChargeState
	 */
	public ChargeState getChargeState() {
		return this.chargeState;
	}

	/**
	 * Set local ChargeState and parent channel.
	 * 
	 * @param chargeState current ChargeState
	 */
	private void setChargeState(ChargeState chargeState) {
		this.chargeState = chargeState;
		this.parent.channel(ManagedEvcs.ChannelId.CHARGE_STATE).setNextValue(chargeState);
	}
}
