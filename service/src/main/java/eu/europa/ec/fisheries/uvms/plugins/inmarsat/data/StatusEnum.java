package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum StatusEnum {
    UNKNOWN('U'),
    PENDING('P'),
    TRANSMITTED('T'),
    FAIL('F'),
    SUCCESSFULL('S');
    private final char value;

    StatusEnum(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }
}
