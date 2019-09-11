/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.data;

import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;

public class InmarsatPoll {

    private String oceanRegion;
    private PollEnum pollEnum;
    private int dnid;
    private ResponseEnum responseEnum;
    private SubAddressEnum subAddressEnum;
    private String address;
    private CommandEnum commandEnum;
    private int memberId;
    private int startFrame;
    private int reportsPerDay;
    private int reportFrequency;
    private String pollId;
    private AckEnum ackEnum;

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
        this.pollId = pollId;
    }

    public String getOceanRegion() {
        return oceanRegion;
    }

    public void setOceanRegion(String oceanRegion) {
        this.oceanRegion = oceanRegion;
    }

    public PollEnum getPollEnum() {
        return pollEnum;
    }

    public void setPollEnum(PollEnum pollEnum) {
        this.pollEnum = pollEnum;
    }

    public int getDnid() {
        return dnid;
    }

    public void setDnid(int dnid) {
        this.dnid = dnid;
    }

    public ResponseEnum getResponseEnum() {
        return responseEnum;
    }

    public void setResponseEnum(ResponseEnum responseEnum) {
        this.responseEnum = responseEnum;
    }

    public SubAddressEnum getSubAddressEnum() {
        return subAddressEnum;
    }

    public void setSubAddressEnum(SubAddressEnum subAddressEnum) {
        this.subAddressEnum = subAddressEnum;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CommandEnum getCommandEnum() {
        return commandEnum;
    }

    public void setCommandEnum(CommandEnum commandEnum) {
        this.commandEnum = commandEnum;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getStartFrame() {
        return startFrame;
    }

    public void setStartFrame(int startFrame) {
        this.startFrame = startFrame;
    }

    public int getReportsPerDay() {
        return reportsPerDay;
    }

    public void setReportsPerDay(int reportsPerDay) {
        this.reportsPerDay = reportsPerDay;
    }

    public AckEnum getAckEnum() {
        return ackEnum;
    }

    public void setAckEnum(AckEnum ackEnum) {
        this.ackEnum = ackEnum;
    }

    public int getReportFrequency() {
        return reportFrequency;
    }

    public void setReportFrequency(int reportFrequency) {
        this.reportFrequency = reportFrequency;
    }

    public void setFieldsFromPoll(PollType poll) {
        poll.getPollPayload();

        setPollEnum(PollEnum.INDV);
        setResponseEnum(ResponseEnum.DATA);
        setSubAddressEnum(SubAddressEnum.THRANE);
        setCommandEnum(CommandEnum.DEMAND_REPORT);

        for(KeyValueType element : poll.getPollReceiver()) {
            if (element.getKey().equalsIgnoreCase("DNID")) {
                setDnid(Integer.parseInt(element.getValue()));
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                setMemberId(Integer.parseInt(element.getValue()));
            } else if (element.getKey().equalsIgnoreCase("SATELLITE_NUMBER")) {
                setAddress(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("REPORT_FREQUENCY")) {
                // TODO: Added reportFrequency field into this class. We will use it maybe later?
                setReportFrequency(Integer.valueOf(element.getValue()));
            }
        }
        setStartFrame(-1);
        setReportsPerDay(-1);
        setAckEnum(AckEnum.FALSE);
        setPollId(poll.getPollId());
    }

    public String asCommand() {
        StringBuilder builder = new StringBuilder();
        final char comma = ',';

        builder.append("POLL ")
               .append(oceanRegion)
               .append(comma).append(getPollEnum().getValue())
               .append(comma).append(getDnid())
               .append(comma).append(getResponseEnum().getValue())
               .append(comma).append(getSubAddressEnum().getValue());

        String cmdAddress = getAddress();

        if (cmdAddress != null) {
            cmdAddress = cmdAddress.replace(" ", "");
        }

        builder.append(comma).append(cmdAddress) //todo: is it ok for this to be null in the command?
               .append(comma).append(getCommandEnum().getValue())
               .append(comma).append(getMemberId());

        builder.append(comma);
        if (getStartFrame() >= 0) {
            builder.append(Integer.toString(getStartFrame()));
        }

        builder.append(comma);
        if (getReportsPerDay() >= 0) {
            builder.append(Integer.toString(getReportsPerDay()));
        }
        builder.append(comma).append(getAckEnum().getValue());
        return builder.toString();
    }
}
