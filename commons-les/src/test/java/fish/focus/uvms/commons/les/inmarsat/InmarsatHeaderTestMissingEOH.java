package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InmarsatHeaderTestMissingEOH {


    public static byte[] removeLast(byte[] arr)
    {
        if (arr == null ) {
            return arr;
        }
        if(arr.length < 2) return arr;
        int newLength = arr.length - 1;
        byte[] newArray = new byte[newLength];
        for (int i = 0, k = 0; i < newLength; i++) {
            newArray[k++] = arr[i];
        }
        return newArray;
    }

    public static void trc (byte[] arr)
    {
        if (arr == null ) {
            return ;
        }
        for (int i = 0, k = 0; i < arr.length; i++) {
            System.out.print(""+ arr[i]);
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

        InmarsatMessage[]  messages = interpreter.byteToInmMessage(fixed);

        assertTrue(messages.length == 1);
        InmarsatMessage msg = messages[0];
        assertTrue(msg.validate());

    }


}
