package io.openems.edge.evcs.ocpp.alfen;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int minHwPower;
		private int maxHwPower;
		private int connectorId;
		private String ocppId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMinHwPower(int minHwCurrent) {
			this.minHwPower = minHwCurrent;
			return this;
		}

		public Builder setMaxHwPower(int maxHwCurrent) {
			this.maxHwPower = maxHwCurrent;
			return this;
		}

		public Builder setConnectorId(int connectorId) {
			this.connectorId = connectorId;
			return this;
		}

		public Builder setOcppId(String ocppId) {
			this.ocppId = ocppId;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ocppId() {
		return this.builder.ocppId;
	}

	@Override
	public int connectorId() {
		return this.builder.connectorId;
	}

	@Override
	public int maxHwPower() {
		return this.builder.maxHwPower;
	}

	@Override
	public int minHwPower() {
		return this.builder.minHwPower;
	}

}