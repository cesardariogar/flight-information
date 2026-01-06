package com.example.FlightInformation.controller;

import com.flightsinfo.tar1090.controller.OpenSkyApiController;
import com.flightsinfo.tar1090.model.BoundingBox;
import com.flightsinfo.tar1090.model.PlaneStates;
import com.flightsinfo.tar1090.model.StateVector;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class TestOpenSkyApi {

    /**
     * credentials to test authenticated API calls
     **/
    static final String USERNAME = null;
    static final String PASSWORD = null;
    // serials which belong to the given account
    static final Integer[] SERIALS = null;

    @Test
    public void testNoMoreThan2RequestsBetween10Secs() throws IOException, InterruptedException {
        OpenSkyApiController api = new OpenSkyApiController();

        long t0 = System.nanoTime();
        PlaneStates os = api.getStates(0, null);

        long t1 = System.nanoTime();
        System.out.println("Request anonStates time = " + ((t1 - t0) / 1000000) + "ms");
        assertTrue("More than 1 state vector", os.getStateVectors().size() > 1);
        int time = os.getTime();

        // more than two requests withing ten seconds
        os = api.getStates(0, null);
        assertNull("No new data", os);

        // wait ten seconds
        Thread.sleep(10000);

        // now we can retrieve states again
        t0 = System.nanoTime();
        os = api.getStates(0, null);
        t1 = System.nanoTime();
        System.out.println("Request anonStates time = " + ((t1 - t0) / 1000000) + "ms");
        assertNotNull(os);
        assertTrue("More than 1 state vector for second valid request", os.getStateVectors().size() > 1);
        assertNotEquals(time, os.getTime());
    }


    @Test
    public void testAnonGetStates() throws IOException, InterruptedException {
        OpenSkyApiController api = new OpenSkyApiController();
        PlaneStates os = api.getStates(0, null);

        // test bounding box around Switzerland
        api = new OpenSkyApiController();

        try {
            api.getStates(0, null, new BoundingBox(145.8389, 47.8229, 5.9962, 10.5226));
            fail("Illegal coordinates should be detected");
        } catch (RuntimeException re) {
            // NOP
        }

        try {
            api.getStates(0, null, new BoundingBox(45.8389, -147.8229, 5.9962, 10.5226));
            fail("Illegal coordinates should be detected");
        } catch (RuntimeException re) {
            // NOP
        }

        try {
            api.getStates(0, null, new BoundingBox(45.8389, 47.8229, 255.9962, 10.5226));
            fail("Illegal coordinates should be detected");
        } catch (RuntimeException re) {
            // NOP
        }

        try {
            api.getStates(0, null, new BoundingBox(45.8389, 47.8229, 5.9962, -180.5226));
            fail("Illegal coordinates should be detected");
        } catch (RuntimeException re) {
            // NOP
        }
        PlaneStates os2 = api.getStates(0, null, new BoundingBox(45.8389, 47.8229, 5.9962, 10.5226));

        assertTrue("Much less states in Switzerland area than world-wide", os2.getStateVectors().size() < os.getStateVectors().size() - 200);
    }

    // can only be tested with a valid account
    @Test
    public void testAuthGetStates() throws IOException, InterruptedException {
        if (USERNAME == null || PASSWORD == null) {
            System.out.println("WARNING: testAuthGetStates needs valid credentials and did not run");
            return;
        }

        OpenSkyApiController api = new OpenSkyApiController(USERNAME, PASSWORD);
        PlaneStates os = api.getStates(0, null);
        assertTrue("More than 1 state vector", os.getStateVectors().size() > 1);
        int time = os.getTime();

        // more than two requests withing ten seconds
        os = api.getStates(0, null);
        assertNull("No new data", os);

        // wait five seconds
        Thread.sleep(5000);

        // now we can retrieve states again
        long t0 = System.nanoTime();
        os = api.getStates(0, null);
        long t1 = System.nanoTime();
        System.out.println("Request authStates time = " + ((t1 - t0) / 1000000) + "ms");
        assertNotNull(os);
        assertTrue("More than 1 state vector for second valid request", os.getStateVectors().size() > 1);
        assertNotEquals(time, os.getTime());
    }

    @Test
    public void testAnonGetMyStates() {
        OpenSkyApiController api = new OpenSkyApiController();
        try {
            api.getMyStates(0, null, null);
            fail("Anonymous access of 'myStates' expected");
        } catch (IllegalAccessError iae) {
            // like expected
            assertTrue("Mismatched exception message",
                    iae.getMessage().equals("Anonymous access of 'myStates' not allowed"));
        } catch (IOException e) {
            fail("Request should not be submitted");
        }
    }

    @Test
    public void testAuthGetMyStates() throws IOException {
        //DEBUG output:
        //		 System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.SimpleLog");
        //		 System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        //		 System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
        //		 System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.conn", "DEBUG");
        //		 System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.client", "DEBUG");
        //		 System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.client", "DEBUG");
        //		 System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
        if (USERNAME == null || PASSWORD == null) {
            System.out.println("WARNING: testAuthGetMyStates needs valid credentials and did not run");
            return;
        }

        OpenSkyApiController api = new OpenSkyApiController(USERNAME, PASSWORD);
        PlaneStates os = api.getMyStates(0, null, SERIALS);
        assertTrue("More than 1 state vector", os.getStateVectors().size() > 1);

        for (StateVector sv : os.getStateVectors()) {
            // all states contain at least one of the user's sensors
            boolean gotOne = false;
            for (Integer ser : SERIALS) {
                gotOne = gotOne || sv.getSerials().contains(ser);
            }
            assertTrue(gotOne);
        }
    }
}
