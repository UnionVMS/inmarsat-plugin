package fish.focus.uvms.commons.les.inmarsat;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InmarsatHeaderTestMissingEOH {



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
        assertTrue(InmarsatHeader.validate(fixed));
    }


}
