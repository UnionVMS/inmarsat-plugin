package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;

import java.util.List;

public abstract class InmarsatPoll {

    final String oceanRegion;

    InmarsatPoll(String oceanRegion) {
        this.oceanRegion = oceanRegion;
    }

    public abstract void setFieldsFromPoll(PollType poll);
    public abstract List<String> asCommand();
}
