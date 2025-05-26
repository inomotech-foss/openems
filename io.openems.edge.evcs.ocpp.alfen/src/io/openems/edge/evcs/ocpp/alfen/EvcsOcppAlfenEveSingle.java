package io.openems.edge.evcs.ocpp.alfen;

import org.osgi.service.event.EventHandler;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.meter.api.ElectricityMeter;

/** Marker-Interface für die Alfen-Komponente. */
public interface EvcsOcppAlfenEveSingle extends Evcs, MeasuringEvcs, ManagedEvcs,
                                                ElectricityMeter, OpenemsComponent,
                                                EventHandler {

    /** Keine Alfen-spezifischen Kanäle nötig. */
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;

        @Override public Doc doc(){ return null; }
    }
}
