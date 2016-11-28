/*
 Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
 © European Union, 2015-2016.

 This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
 redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
 the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
 copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import static junit.framework.Assert.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class TestInmBody {

    @Test
    public void testGetPositionDate_DateAfterNowTooManyDays() {
        InmBody body = new InmBody(new byte[] {}) {
            @Override public int getDayOfMonth() { return 31; }
            @Override public int getHour() { return 13; }
            @Override public int getMinutes() { return 30; }
        };

        DateTime date = body.getPositionDate(new DateTime(2016,11,28,14,30, DateTimeZone.UTC));

        assertEquals(10, date.monthOfYear().get());
    }

    @Test
    public void testGetPositionDate_DateAfterNow() {
        InmBody body = new InmBody(new byte[] {}) {
            @Override public int getDayOfMonth() { return 30; }
            @Override public int getHour() { return 13; }
            @Override public int getMinutes() { return 30; }
        };

        DateTime date = body.getPositionDate(new DateTime(2016,11,28,14,30, DateTimeZone.UTC));

        assertEquals(10, date.monthOfYear().get());
    }

    @Test
    public void testGetPositionDate_DateAfterNowYearShift() {
        InmBody body = new InmBody(new byte[] {}) {
            @Override public int getDayOfMonth() { return 31; }
            @Override public int getHour() { return 13; }
            @Override public int getMinutes() { return 30; }
        };

        DateTime date = body.getPositionDate(new DateTime(2016,1,28,14,30, DateTimeZone.UTC));

        assertEquals(12, date.monthOfYear().get());
        assertEquals(2015, date.year().get());
    }
}
