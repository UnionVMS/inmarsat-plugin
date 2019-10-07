package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;

import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class ConfigPoll extends InmarsatPoll {

    public ConfigPoll(String oceanRegion) {
        super(oceanRegion);
    }

    /** 12 fields of Poll Command in order */
    private PollEnum pollType;
    private int dnid;
    private ResponseEnum responseType;
    private SubAddressEnum subAddress;  // default = 0?
    private String address;
    private CommandEnum commandType;
    private int memberNumber = 1; // default
    private String startFrame = "0"; // default
    private int reportsPer24hours = 10; // default
    private AckEnum acknowledgement; // default 0 (FALSE)
    private int spotId = 0; // default
    private String mesSerialNumber = ""; // default
    /** end of poll commands */
    private String pollId;

    private int gracePeriod; // Not clear what data type this should be
    private int inPortGrace; // Not clear what data type this should be
    private int startHour;
    private int startMinute;

    @Override
    public void setFieldsFromPoll(PollType poll) {
        pollType = PollEnum.INDV;
        responseType = ResponseEnum.DATA;
        subAddress = SubAddressEnum.THRANE;
        commandType = CommandEnum.DEMAND_REPORT;
        acknowledgement = AckEnum.TRUE;
        pollId = poll.getPollId();

        for (KeyValueType element : poll.getPollReceiver()) {
            if (element.getKey().equalsIgnoreCase("DNID")) {
                dnid = Integer.parseInt(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("SATELLITE_NUMBER")) {
                address = element.getValue();
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                memberNumber = Integer.parseInt(element.getValue());
            }  else if (element.getKey().equalsIgnoreCase("REPORT_FREQUENCY")) {
                reportsPer24hours = Integer.parseInt(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("GRACE_PERIOD")) {
                gracePeriod = Integer.parseInt(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("IN_PORT_GRACE")) {
                inPortGrace = Integer.parseInt(element.getValue());
            }
        }
    }

    @Override
    public List<String> asCommand() {
        String stop = buildStopIndividualPoll();
        String config = buildConfigIndividualPoll();
        String start = buildStartIndividualPoll();
        return Arrays.asList(stop, config, start);
    }

/**
        P01 - PollEnum pollType;
        P02 - int dnid;
        P03 - ResponseEnum responseType;
        P04 - SubAddressEnum subAddress;  // default = 0?
        P05 - String address;
        P06 - CommandEnum commandType;
        P07 - int memberNumber = 1; // default
        P08 - int startFrame = 0; // default
        P09 - int reportsPer24hours = 10; // default
        P10 - AckEnum acknowledgement; // default 0 (FALSE)
        P11 - int spotId = 0; // default
        P12 - String mesSerialNumber = ""; // default
*/
    public String buildStopIndividualPoll() {
        return String.format("poll %s,I,%s,N,1,%s,6", oceanRegion, dnid, address);
    }

    public String buildConfigIndividualPoll() {
        startFrame = calcStartFrame(startHour, startMinute);
        return String.format("poll %s,I,%s,N,1,%S,4,,%s,%s", oceanRegion, dnid, address, startFrame, reportsPer24hours);
    }

    public String buildStartIndividualPoll() {
        return String.format("poll %s,I,%s,D,1,%s,5", oceanRegion, dnid, address);
    }

    /**
     * (((hour * 60) + minute) * 60) / 8.64 = startFrame number.
     * Night and Day (24 hours) are divided in 10000 frames equal to 8,64 second.
     * Example: 24 * 60 * 60 / 10000 = 8,64
     * The "24" at the end indicate that the terminals shall send one report every hour.
     * Example: 13:28 o'clock will be equal to 5611
     *
     * @param hour   Hours of the day (24 hours format)
     * @param minute Minutes of the hour
     * @return startFrame
     */
    private String calcStartFrame(int hour, int minute) {
        if ((hour < 0) || (hour > 24)) {
            throw new IllegalArgumentException("Hour must be between 0 and 24. Was " + hour);
        }
        if ((minute < 0) || (minute > 60)) {
            throw new IllegalArgumentException("Minute must be between 0 and 60. Was " + minute);
        }

        int value = (int) ((((hour * 60) + minute) * 60) / 8.64);
        return String.valueOf(value);
    }

}
