package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

public enum PollEnum {
    GROUP('G'),
    INDV('I'),
    NAVAREA('N'),
    RECT('R'),
    CIRC('C');
    private final char value;

    PollEnum(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }
}