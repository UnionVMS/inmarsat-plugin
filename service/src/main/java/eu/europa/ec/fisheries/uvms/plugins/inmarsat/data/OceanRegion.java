package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum OceanRegion {
    AORW(0),
    AORE(1),
    POR(2),
    IOR(3);
    private final int value;

    OceanRegion(int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }
}
