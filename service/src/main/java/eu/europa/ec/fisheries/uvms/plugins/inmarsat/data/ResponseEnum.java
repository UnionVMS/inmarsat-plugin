package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum ResponseEnum {
    DATA('D'),
    MSG('M'),
    NO_RESP('N');
    private final char value;

    ResponseEnum(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }
}
