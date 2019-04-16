package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InmarsatHeaderTestMissingEOH {


    public static byte[] removeLast(byte[] arr) {
        if (arr == null) {
            return arr;
        }
        if (arr.length < 2) return arr;
        int newLength = arr.length - 1;
        byte[] newArray = new byte[newLength];
        for (int i = 0, k = 0; i < newLength; i++) {
            newArray[k++] = arr[i];
        }
        return newArray;
    }

    public static void trc(byte[] arr) {
        if (arr == null) {
            return;
        }
        for (int i = 0, k = 0; i < arr.length; i++) {
            System.out.print("" + arr[i]);
        }
        System.out.println("");
    }


    @Test
    public void isValidHeaderOk() {
        byte[] aMessage = InmarsatUtils.hexStringToByteArray("015426540116890b08000255140036372455c307e702");
        assertTrue(InmarsatHeader.validate(aMessage));
    }

    @Test
    public void isValidHeaderNoEOHAtExcpectedPosition() {
        byte[] aMessage = InmarsatUtils.hexStringToByteArray("015426540116890b08000255140036372455c307e702");
        aMessage[21] = 42;
        assertFalse(InmarsatHeader.validate(aMessage));
    }

    @Test
    public void repairErrouneusHead() {

        InmarsatInterpreter interpreter = new InmarsatInterpreter();

        byte[] aMessageHeader = InmarsatUtils.hexStringToByteArray("015426540116890b08000255140036372455c307e703");
        byte[] fixed = interpreter.insertMissingData(aMessageHeader);

        // test that last token in the old array is the previous erroneus token that should not be in the head
        byte notfixed_token = aMessageHeader[aMessageHeader.length - 1];
        byte fixed_token = fixed[fixed.length - 2];
        assertTrue(notfixed_token != fixed_token);

        // remove the last token that not belongs to the header, so we can validate the header
        fixed = removeLast(fixed);
        assertTrue(InmarsatHeader.validate(fixed));
    }

    @Test
    public void repairErrorInHead() {

        InmarsatInterpreter interpreter = new InmarsatInterpreter();
        // 015426540116890b08000255140036372455c307e703
        byte[] aMessageHeader = InmarsatUtils.hexStringToByteArray("015426540116890b08000255140036372455c307e703010203040506");
        byte[] fixed = interpreter.insertMissingData(aMessageHeader);

        InmarsatMessage[] messages = interpreter.byteToInmMessage(fixed);

        assertTrue(messages.length == 1);
        InmarsatMessage msg = messages[0];
        assertTrue(msg.validate());

    }

    @Test
    public void repairErrorInHeadBiggerMessage() {

        InmarsatInterpreter interpreter = new InmarsatInterpreter();
        byte[] aMessageHeader = InmarsatUtils.hexStringToByteArray("0D0A52657472696576696E6720444E494420646174612E2E2E0D0A0154265401166EFA000002C41400AF84B45C832A0F024F383044908B3DA800960000458000000000000001542654011651B50A0002C41400B784B45C832A11024E6CA042860B3DA8006C800045800000000000000154265401162137040002C41400C084B45C832A13024E5BA81CC70B3DA8123800004580000000000000015426540116911F070002C41400C084B45C832A16024E75A026688B3DA82B1D8000458000000000000001542654011629CC000002C41400C084B45C832A14024E09983E218B3DA8004D000045800000000000000154265401169D3B0F0002C41400C084B45C832A0E024DDB403B3B8B3DA8142C000045800000000000000154265401165532030002C41400FC84B45C832A4E026AB82FA50B3DA9002A000045800000000000000D0A3E20");
        byte[] fixed = interpreter.insertMissingData(aMessageHeader);

        InmarsatMessage[] messages = interpreter.byteToInmMessage(fixed);
        int n = messages.length;
        assertTrue(n > 1);
        for(int i = 0 ; i < n ; i++) {
            InmarsatMessage msg = messages[i];
            assertTrue(msg.validate());
        }

    }


    @Test
    public void repairErrorInHeadBiggerMessage_2_() {

        InmarsatInterpreter interpreter = new InmarsatInterpreter();
        byte[] aMessageHeader = InmarsatUtils.hexStringToByteArray(
                "0d0a52657472696576696e6720444e494420646174612e2e2e0d0a015426540116f3d9030002c414001c67b55c832a4e6ab82fa50b40ab006100004580000000000000015426540116bd38000002c414007367b55c832a13024e5b301da38b40ac0d2a800045800000000000000154265401168a59020002c414007367b55c832a0e024ddcb03a6c0b40ac1c0d458000000000000001542654011654730b0002c414007367b55c832a0f024f383044900b40ac009180004580000000000000015426540116990002c414007367b55c832a14024e09983e218b40ac00b28000458000000000000001542654011668d9020002c414007367b55c832a11024e6ca842860b40ac00a7000045800000000000000d0a3e20");
        byte[] fixed = interpreter.insertMissingData(aMessageHeader);

        // this one is taken from errorlog and we want to check why its found there
        byte[] afterCorr = InmarsatUtils.hexStringToByteArray("0d0a52657472696576696e6720444e494420646174612e2e2e0d0a015426540116f3d9030002c414001c67b55c832a4e026ab82fa50b40ab006100004580000000000000015426540116bd38000002c414007367b55c832a13024e5b301da38b40ac0d2a800045800000000000000154265401168a59020002c414007367b55c832a0e024ddcb03a6c0b40ac1c0d458000000000000001542654011654730b0002c414007367b55c832a0f024f383044900b40ac00918000458000000000000001542654011699000200c41400730067b55c832a14024e0209983e218b40ac00b28000458000000000000001542654011668d9020002c414007367b55c832a11024e6ca842860b40ac00a7000045800000000000000d0a3e20");

        InmarsatMessage[] messages = interpreter.byteToInmMessage(fixed);
        int n = messages.length;
        assertTrue(n > 1);
        for (int i = 0; i < n; i++) {
            InmarsatMessage msg = messages[i];
            assertTrue(msg.validate());
        }

        InmarsatMessage[] messages2 = interpreter.byteToInmMessage(afterCorr);
        int n2 = messages2.length;
        assertTrue(n2 > 1);
        for (int i = 0; i < n2; i++) {
            InmarsatMessage msg = messages2[i];
            assertTrue(msg.validate());
        }

        String fromErrorLog =  new String(afterCorr);
        String afterFixLocal =  new String(fixed);
        assertTrue(fromErrorLog.equals(afterFixLocal));





    }



}
