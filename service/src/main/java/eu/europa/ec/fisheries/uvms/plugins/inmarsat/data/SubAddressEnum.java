package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum SubAddressEnum {
    TRIMBLE(0),
    THRANE(1),
    TELESYSTEMS(7);
    private final int value;

    SubAddressEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
