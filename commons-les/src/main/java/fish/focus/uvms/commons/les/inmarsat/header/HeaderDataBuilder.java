package fish.focus.uvms.commons.les.inmarsat.header;

import fish.focus.uvms.commons.les.inmarsat.InmarsatDefinition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeaderDataBuilder {
	private HeaderType type;
	private int refno;
	private HeaderDataPresentation dataPresentation;
	private int failureReason;
	private int deliveryAttempts;
	private int satIdAndLesId;
	private Date storedTime;
	private int dataLength;
	private int dnid;
	private int memNo;
	private int mesNo;

	public HeaderDataBuilder setType(HeaderType type) {
		this.type = type;
		return this;
	}

	public HeaderDataBuilder setRefno(int refno) {
		this.refno = refno;
		return this;
	}

	public HeaderDataBuilder setDataPresentation(HeaderDataPresentation dataPresentation) {
		this.dataPresentation = dataPresentation;
		return this;
	}

	public HeaderDataBuilder setFailureReason(int failureReason) {
		this.failureReason = failureReason;
		return this;
	}

	public HeaderDataBuilder setDeliveryAttempts(int deliveryAttempts) {
		this.deliveryAttempts = deliveryAttempts;
		return this;
	}

	public HeaderDataBuilder setSatIdAndLesId(int satIdAndLesId) {
		this.satIdAndLesId = satIdAndLesId;
		return this;
	}

	@SuppressWarnings("WeakerAccess")
	public HeaderDataBuilder setStoredTime(Date storedTime) {
		this.storedTime = storedTime;
		return this;
	}

	public HeaderDataBuilder setStoredTime(String storedTime) throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(InmarsatDefinition.API_TIMEZONE);
		return setStoredTime(sdf.parse(storedTime));
	}

	public HeaderDataBuilder setDataLength(int dataLength) {
		this.dataLength = dataLength;
		return this;
	}

	public HeaderDataBuilder setDnid(int dnid) {
		this.dnid = dnid;
		return this;
	}

	public HeaderDataBuilder setMemNo(int memNo) {
		this.memNo = memNo;
		return this;
	}

	public HeaderDataBuilder setMesNo(int mesNo) {
		this.mesNo = mesNo;
		return this;
	}

	public HeaderData createHeaderData() {
		return new HeaderData(type, refno,
				new HeaderData.OptionalHeader1(dataPresentation, failureReason, deliveryAttempts, satIdAndLesId),
				storedTime, new HeaderData.OptionalHeader2(dataLength, dnid, memNo, mesNo));
	}
}
