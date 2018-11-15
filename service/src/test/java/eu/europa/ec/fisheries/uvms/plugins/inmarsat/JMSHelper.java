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

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class JMSHelper {



    private static final String INMARSAT_EVENTBUS_NAME = "EventBus";


    private ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:18161");


    public String sendMessage(String text) throws Exception {
        Connection connection = connectionFactory.createConnection();
        try {
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(INMARSAT_EVENTBUS_NAME);
            TextMessage message = session.createTextMessage();
            message.setText(text);
            session.createProducer(queue).send(message);
            return message.getJMSMessageID();
        } finally {
            connection.close();
        }
    }


}
