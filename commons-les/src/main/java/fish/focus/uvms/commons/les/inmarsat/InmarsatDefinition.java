/*
 * ﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
 * © European Union, 2015-2016.
 * 
 * This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU
 * General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.commons.les.inmarsat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class InmarsatDefinition {
	public static final Charset API_CHARSET = StandardCharsets.US_ASCII;
	public static final TimeZone API_TIMEZONE = TimeZone.getTimeZone("UTC");

	// API Misc. definitions
	public static final int API_SOH = 1;
	public static final int API_EOH = 2;
	public static final String API_LEAD_TEXT = "T&T";
	// API Reason code definitions
	public static final int API_UNKNOWN_ERROR = 0x00;
	public static final int API_MES_UNKNOWN = 0x01;
	public static final int API_MES_LOGGED_OUT = 0x02;
	public static final int API_MES_NOT_IN_OR = 0x03;
	public static final int API_MES_BARRED = 0x04;
	public static final int API_MES_NOT_ANSWERING = 0x05;
	public static final int API_MES_NOT_COMMISSIONED = 0x06;
	public static final int API_MSG_TIMED_OUT = 0x10;
	public static final int API_MSG_DELETED_BY_OPERATOR = 0x12;
	public static final int API_MSG_DELETED_BY_USER = 0x13;
	public static final int API_MSG_OR_NOT_COVERED = 0x14;

	private InmarsatDefinition() {}
}
