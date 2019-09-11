package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum AckEnum {
    FALSE(0),
    TRUE(1);
    private final int value;

    AckEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
