package fish.focus.uvms.commons.les.inmarsat.header;

public class HeaderStructBuilder {
	private boolean presentation = true;
	private boolean failure = true;
	private boolean delivery = true;
	private boolean satIdAndLesId = true;
	private boolean dataLength = true;
	private boolean dnid = true;
	private boolean memberNo = true;
	private boolean mesMobNo = true;

	public HeaderStructBuilder enableAll() {
		presentation = true;
		failure = true;
		delivery = true;
		satIdAndLesId = true;
		dataLength = true;
		dnid = true;
		memberNo = true;
		mesMobNo = true;
		return this;

	}

	public HeaderStructBuilder disableAll() {
		presentation = false;
		failure = false;
		delivery = false;
		satIdAndLesId = false;
		dataLength = false;
		dnid = false;
		memberNo = false;
		mesMobNo = false;
		return this;

	}

	public HeaderStructBuilder enablePresentation() {
		presentation = true;
		return this;
	}

	public HeaderStructBuilder enableFailure() {
		failure = true;
		return this;
	}

	public HeaderStructBuilder enableDelivery() {
		delivery = true;
		return this;
	}

	public HeaderStructBuilder enableSatIdAndLesId() {
		satIdAndLesId = true;
		return this;
	}

	public HeaderStructBuilder enableDataLength() {
		dataLength = true;
		return this;
	}

	public HeaderStructBuilder enableDnid() {
		dnid = true;
		return this;
	}

	public HeaderStructBuilder enableMemberNo() {
		memberNo = true;
		return this;
	}

	public HeaderStructBuilder enableMesMobNo() {
		mesMobNo = true;
		return this;
	}


	public HeaderStructBuilder disablePresentation() {
		presentation = false;
		return this;
	}

	public HeaderStructBuilder disableFailure() {
		failure = false;
		return this;
	}

	public HeaderStructBuilder disableDelivery() {
		delivery = false;
		return this;
	}

	public HeaderStructBuilder disableSatIdAndLesId() {
		satIdAndLesId = false;
		return this;
	}

	public HeaderStructBuilder disableDataLength() {
		dataLength = false;
		return this;
	}

	public HeaderStructBuilder disableDnid() {
		dnid = false;
		return this;
	}

	public HeaderStructBuilder disableMemberNo() {
		memberNo = false;
		return this;
	}

	public HeaderStructBuilder disableMesMobNo() {
		mesMobNo = false;
		return this;
	}

	public HeaderStruct createHeaderStruct() {
		return new HeaderStruct(new HeaderStruct.Part1(presentation, failure, delivery, satIdAndLesId, dataLength),
				new HeaderStruct.Part2(dnid, memberNo, mesMobNo));
	}
}
