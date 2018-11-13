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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.KeyValueType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PollType;
import java.util.Iterator;

/** */
public class InmarsatPoll {

  private OceanRegion oceanRegion;
  private Poll_Type pollType;
  private int dnind;
  private ResponseType responseType;
  private SubAdressType subAddress;
  private String adress;
  private CommandType command;
  private int memberId;
  private int startFrame;
  private int reportsPerDay;
  private String pollId;
  private AckType ack;

  public String getPollId() {
    return pollId;
  }

  public void setPollId(String pollId) {
    this.pollId = pollId;
  }

  public OceanRegion getOceanRegion() {
    return oceanRegion;
  }

  public void setOceanRegion(OceanRegion oceanRegion) {
    this.oceanRegion = oceanRegion;
  }

  public Poll_Type getPollType() {
    return pollType;
  }

  public void setPollType(PollType poll) {
    poll.getPollPayload();
    Iterator itr = poll.getPollReceiver().iterator();

    setPollType(Poll_Type.INDV);
    setResponseType(ResponseType.DATA);
    setSubAddress(SubAdressType.THRANE);
    setCommand(CommandType.DEMAND_REPORT);
    while (itr.hasNext()) {
      KeyValueType element = (KeyValueType) itr.next();

      if (element.getKey().equalsIgnoreCase("DNID")) {
        setDnind(Integer.parseInt(element.getValue()));
      } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
        setMemberId(Integer.parseInt(element.getValue()));
      } else if (element.getKey().equalsIgnoreCase("SATELLITE_NUMBER")) {
        setAdress(element.getValue());
      } else if (element.getKey().equalsIgnoreCase("REPORT_FREQUENCY")) {
        // TODO - Should this be removed?
      }
    }
    setStartFrame(-1);
    setReportsPerDay(-1);
    setAck(AckType.FALSE);
    setPollId(poll.getPollId());
  }

  private void setPollType(Poll_Type pollType) {
    this.pollType = pollType;
  }

  public int getDnind() {
    return dnind;
  }

  public void setDnind(int dnind) {
    this.dnind = dnind;
  }

  public ResponseType getResponseType() {
    return responseType;
  }

  public void setResponseType(ResponseType responseType) {
    this.responseType = responseType;
  }

  public SubAdressType getSubAddress() {
    return subAddress;
  }

  public void setSubAddress(SubAdressType subAddress) {
    this.subAddress = subAddress;
  }

  public String getAdress() {
    return adress;
  }

  public void setAdress(String adress) {
    this.adress = adress;
  }

  public CommandType getCommand() {
    return command;
  }

  public void setCommand(CommandType command) {
    this.command = command;
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

  public AckType getAck() {
    return ack;
  }

  public void setAck(AckType ack) {
    this.ack = ack;
  }

  public String asCommand() {
    String cmd = "POLL ";
    cmd += Integer.toString(getOceanRegion().getValue());
    cmd += "," + getPollType().getValue();
    cmd += "," + getDnind();
    cmd += "," + getResponseType().getValue();
    cmd += "," + getSubAddress().getValue();

    String cmdAdress = getAdress();

    if (cmdAdress != null) {
      cmdAdress = cmdAdress.replace(" ", "");
    }

    cmd += "," + cmdAdress;
    cmd += "," + getCommand().getValue();
    cmd += "," + getMemberId();
    cmd += ",";
    if (getStartFrame() >= 0) {
      cmd += Integer.toString(getStartFrame());
    }
    cmd += ",";
    if (getReportsPerDay() >= 0) {
      cmd += Integer.toString(getReportsPerDay());
    }
    cmd += "," + getAck().getValue();
    return cmd;
  }

  public enum OceanRegion {
    AORE(1),
    AORW(0),
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

  public enum Poll_Type {
    GROUP('G'),
    INDV('I'),
    NAVAREA('N'),
    RECT('R'),
    CIRC('C');
    private final char value;

    Poll_Type(char value) {
      this.value = value;
    }

    char getValue() {
      return value;
    }
  }

  public enum ResponseType {
    DATA('D'),
    MSG('M'),
    NO_RESP('N');
    private final char value;

    ResponseType(char value) {
      this.value = value;
    }

    char getValue() {
      return value;
    }
  }

  public enum CommandType {
    DEMAND_REPORT("0"),
    INTERVALL("4"),
    START("5"),
    STOP("6");
    private final String value;

    CommandType(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  public enum AckType {
    FALSE(0),
    TRUE(1);
    private final int value;

    AckType(int value) {
      this.value = value;
    }

    int getValue() {
      return value;
    }
  }

  public enum SubAdressType {
    TRIMBLE(0),
    THRANE(1),
    TELESYSTEMS(7);
    private final int value;

    SubAdressType(int value) {
      this.value = value;
    }

    int getValue() {
      return value;
    }
  }
}
