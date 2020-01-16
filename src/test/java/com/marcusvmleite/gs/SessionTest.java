package com.marcusvmleite.gs;

import org.junit.Test;

import java.net.Socket;
import java.util.UUID;

import static org.junit.Assert.*;

public class SessionTest {

    @Test
    public void testProcessGreetingMessage() {
        Session session = new Session(new Socket(), ObjectUtil.getClearGraph());

        String clientName = UUID.randomUUID().toString();

        assertEquals(String.format(Messages.GREETING_REPLY.message(), clientName),
                session.processGreetingMessage("HI, I AM " + clientName).getValue());
        assertEquals(Boolean.TRUE, session.processGreetingMessage("HI, I AM " + UUID.randomUUID().toString()).getKey());
        assertEquals(Boolean.TRUE, session.processGreetingMessage("UNKNOWN").getKey());
        assertEquals(Boolean.FALSE, session.processGreetingMessage("BYE MATE!").getKey());
        assertEquals(Messages.SORRY.message(), session.processGreetingMessage("UNKNOWN").getValue());
    }

    @Test
    public void testProcessGraphMessage() {
        Session session = new Session(new Socket(), ObjectUtil.getClearGraph());

        assertEquals(Messages.NODE_ADDED.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_1));
        assertEquals(Messages.NODE_EXISTS.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_1));
        assertEquals(Messages.NODE_ADDED.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_2));
        assertEquals(Messages.NODE_ADDED.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_3));
        assertEquals(Messages.NODE_EXISTS.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_2));
        assertEquals(Messages.NODE_EXISTS.message(), session.processGraphMessage("ADD NODE " + GraphTest.NODE_TEST_3));

        assertEquals(Messages.EDGE_ADDED.message(), session.processGraphMessage("ADD EDGE " +
                GraphTest.NODE_TEST_3 + " " + GraphTest.NODE_TEST_2 + " " + 1));
        assertEquals(Messages.NODE_NOT_FOUND.message(), session.processGraphMessage("ADD EDGE " +
                GraphTest.UNKNOWN + " " + GraphTest.NODE_TEST_2 + " " + 1));
        assertEquals(Messages.EDGE_REMOVED.message(), session.processGraphMessage("REMOVE EDGE " +
                GraphTest.NODE_TEST_3 + " " + GraphTest.NODE_TEST_2));
        assertEquals(Messages.EDGE_REMOVED.message(), session.processGraphMessage("REMOVE EDGE " +
                GraphTest.NODE_TEST_3 + " " + GraphTest.NODE_TEST_2));

        assertEquals(Messages.EDGE_ADDED.message(), session.processGraphMessage("ADD EDGE " +
                GraphTest.NODE_TEST_1 + " " + GraphTest.NODE_TEST_2 + " " + 1));
        assertEquals(Messages.EDGE_ADDED.message(), session.processGraphMessage("ADD EDGE " +
                GraphTest.NODE_TEST_1 + " " + GraphTest.NODE_TEST_3 + " " + 2));
        assertEquals(Messages.EDGE_ADDED.message(), session.processGraphMessage("ADD EDGE " +
                GraphTest.NODE_TEST_2 + " " + GraphTest.NODE_TEST_3 + " " + 3));

        assertEquals("1", session.processGraphMessage("SHORTEST PATH " +
                GraphTest.NODE_TEST_1 + " " + GraphTest.NODE_TEST_2));
        assertEquals("2", session.processGraphMessage("SHORTEST PATH " +
                GraphTest.NODE_TEST_1 + " " + GraphTest.NODE_TEST_3));
        assertEquals(Integer.MAX_VALUE + "", session.processGraphMessage("SHORTEST PATH " +
                GraphTest.NODE_TEST_3 + " " + GraphTest.NODE_TEST_1));

        assertEquals("NODE-TEST-2,NODE-TEST-3", session.processGraphMessage("CLOSER THAN " +
                5 + " " + GraphTest.NODE_TEST_1));
    }

}
