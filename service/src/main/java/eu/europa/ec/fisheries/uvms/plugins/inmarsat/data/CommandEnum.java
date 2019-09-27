package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum CommandEnum {
    DEMAND_REPORT("0"),
    INTERVAL("4"),
    START("5"),
    STOP("6");
    private final String value;

    CommandEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
