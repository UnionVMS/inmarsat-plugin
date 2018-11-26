package fish.focus.uvms.commons.les.inmarsat.header;

import java.util.Date;

public class HeaderData {
	private final HeaderType type;
	private final int refno;
	private final HeaderDataPresentation dataPresentation;
	private final int failureReason;
	private final int deliveryAttempts;
	private final int satIdAndLesId;
	private final Date storedTime;
	private final int dataLength;
	private final int dnid;
	private final int memNo;
	private final int mesNo;

	public HeaderData(HeaderType type, int refno, OptionalHeader1 optionalHeader1, Date storedTime,
			OptionalHeader2 optionalHeader2) {
		this.type = type;
		this.refno = refno;

		this.dataPresentation = optionalHeader1.getDataPresentation();
		this.failureReason = optionalHeader1.getFailureReason();
		this.deliveryAttempts = optionalHeader1.getDeliveryAttempts();
		this.satIdAndLesId = optionalHeader1.getSatIdAndLesId();
		this.dataLength = optionalHeader2.getDataLength();
		this.storedTime = storedTime;

		this.dnid = optionalHeader2.getDnid();
		this.memNo = optionalHeader2.getMemNo();
		this.mesNo = optionalHeader2.getMesNo();

	}

	public HeaderType getType() {
		return type;
	}

	public int getRefno() {
		return refno;
	}

	public HeaderDataPresentation getDataPresentation() {
		return dataPresentation;
	}

	public int getFailureReason() {
		return failureReason;
	}

	public int getDeliveryAttempts() {
		return deliveryAttempts;
	}

	public int getSatIdAndLesId() {
		return satIdAndLesId;
	}

	public Date getStoredTime() {
		return storedTime;
	}

	public int getDataLength() {
		return dataLength;
	}

	public int getDnid() {
		return dnid;
	}

	public int getMemNo() {
		return memNo;
	}

	public int getMesNo() {
		return mesNo;
	}

	static class OptionalHeader1 {
		private final HeaderDataPresentation dataPresentation;
		private final int failureReason;
		private final int deliveryAttempts;
		private final int satIdAndLesId;

		OptionalHeader1(HeaderDataPresentation dataPresentation, int failureReason, int deliveryAttempts,
				int satIdAndLesId) {
			this.dataPresentation = dataPresentation;
			this.failureReason = failureReason;
			this.deliveryAttempts = deliveryAttempts;
			this.satIdAndLesId = satIdAndLesId;
		}

		public HeaderDataPresentation getDataPresentation() {
			return dataPresentation;
		}

		public int getFailureReason() {
			return failureReason;
		}

		public int getDeliveryAttempts() {
			return deliveryAttempts;
		}

		public int getSatIdAndLesId() {
			return satIdAndLesId;
		}
	}


	static class OptionalHeader2 {
		private final int dataLength;
		private final int dnid;
		private final int memNo;
		private final int mesNo;

		OptionalHeader2(int dataLength, int dnid, int memNo, int mesNo) {
			this.dataLength = dataLength;
			this.dnid = dnid;
			this.memNo = memNo;
			this.mesNo = mesNo;
		}

		public int getDataLength() {
			return dataLength;
		}

		public int getDnid() {
			return dnid;
		}

		public int getMemNo() {
			return memNo;
		}

		public int getMesNo() {
			return mesNo;
		}
	}
}
