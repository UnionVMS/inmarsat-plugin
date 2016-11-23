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
package eu.europa.ec.fisheries.uvms.plugins.inmarsat.twostage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import javax.ejb.AsyncResult;

import org.junit.Before;
import org.junit.Test;

import eu.europa.ec.fisheries.uvms.plugins.inmarsat.StartupBean;

public class TestRetrieverBean {

    RetriverBean retriever;

    @Before
    public void before() {
        retriever = new RetriverBean();
        retriever.downloadService = mock(DownLoadService.class);
        when(retriever.downloadService.download(null, Arrays.asList("DNID-123", "123456", "ABC"))).thenReturn(new AsyncResult<Map<String,String>>(null));
        retriever.startUp = mock(StartupBean.class);
        when(retriever.startUp.getSetting("DNIDS")).thenReturn("DNID-123,123456,ABC,");
        when(retriever.startUp.isIsEnabled()).thenReturn(true);
    }

    @Test
    public void testDownload() {
        retriever.connectAndRetrive();
        verify(retriever.startUp, times(1)).getSetting("DNIDS");
        verify(retriever.downloadService, times(1)).download(null, Arrays.asList("DNID-123", "123456", "ABC"));
    }

    @Test
    public void testNoDinds() {
        when(retriever.startUp.getSetting("DNIDS")).thenReturn(null);
        retriever.connectAndRetrive();
        verify(retriever.startUp, times(1)).getSetting("DNIDS");
    }

    @Test
    public void testBlankDinds() {
        when(retriever.startUp.getSetting("DNIDS")).thenReturn("  ");
        retriever.connectAndRetrive();
        verify(retriever.startUp, times(1)).getSetting("DNIDS");
    }

    @Test
    public void testSingleDind() {
        when(retriever.startUp.getSetting("DNIDS")).thenReturn("123");
        retriever.connectAndRetrive();
        verify(retriever.startUp, times(1)).getSetting("DNIDS");
        verify(retriever.downloadService, times(1)).download(null, Arrays.asList("123"));
    }

    @Test
    public void testDuplicateDnids() {
        when(retriever.startUp.getSetting("DNIDS")).thenReturn("ABC,ABC,");
        retriever.connectAndRetrive();
        verify(retriever.startUp, times(1)).getSetting("DNIDS");
        verify(retriever.downloadService, times(1)).download(null, Arrays.asList("ABC"));
    }
    
}