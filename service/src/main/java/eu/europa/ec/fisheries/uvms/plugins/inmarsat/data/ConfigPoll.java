package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;

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
    private int startFrame = 0; // default
    private int reportsPer24hours = 10; // default
    private AckEnum acknowledgement; // default 0 (FALSE)
    private int spotId = 0; // default
    private String mesSerialNumber = ""; // default
    /** end of poll commands */
    private String pollId;

    // Todo: Make use of these values somehow?
    private int gracePeriod;
    private int inPortGrace;

    @Override
    public void setFieldsFromPoll(PollType poll) {
        poll.getPollPayload();

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
    public String asCommand() {
        StringBuilder builder = new StringBuilder();

        builder.append("POLL ")
                .append(oceanRegion)
                .append(',')
                .append(pollType.getValue())
                .append(',')
                .append(dnid)
                .append(',')
                .append(responseType.getValue())
                .append(',')
                .append(subAddress.getValue());

        if (address != null)
            address = address.replace(" ", "");

        builder.append(',')
                .append(address)
                .append(',')
                .append(commandType.getValue())
                .append(',')
                .append(memberNumber)
                .append(',')
                .append(startFrame)
                .append(',')
                .append(reportsPer24hours)
                .append(',')
                .append(acknowledgement.getValue());
//                .append(',')  // These values are currently not used. Leaving here for reference
//                .append(spotId)
//                .append(',')
//                .append(mesSerialNumber);
        return builder.toString();
    }


}
