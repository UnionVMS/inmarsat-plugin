package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;

public abstract class InmarsatPoll {

    final String oceanRegion;

    InmarsatPoll(String oceanRegion) {
        this.oceanRegion = oceanRegion;
    }

    public abstract void setFieldsFromPoll(PollType poll);
    public abstract String asCommand();
}
