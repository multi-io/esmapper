package de.olafklischat.esmapper;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESRunnerTest {

    private static ESRunner esr;

    @BeforeClass
    public static void setUpDB() throws Exception {
        esr = new ESRunner(6789);
    }
    
    @Test
    public void testConfig() {
    }

    @Test
    public void testStartLocally() throws Exception {
        assertFalse(esr.isRunning());
        assertFalse(esr.isRunningLocally());

        esr.setConfigPath("esrunner");
        System.out.println(esr.getConfig("index.number_of_shards"));
        esr.startLocally();
        System.out.println(esr.getConfig("node.name"));
        System.out.println(esr.getConfig("http.port"));
        System.out.println(esr.getConfig("transport.tcp.port"));
        System.out.println(esr.getConfig("path.data"));
        System.out.println(esr.getConfig("path.work"));
        System.out.println(esr.getConfig("path.logs"));
        System.out.println(esr.getConfig("index.number_of_shards"));
        System.out.println(esr.getClusterName());

        //Thread.sleep(6000);
        assertTrue(esr.isRunning());
        assertTrue(esr.isRunningLocally());
        esr.stopLocally();
        assertFalse(esr.isRunning());
        assertFalse(esr.isRunningLocally());
    }
    
    @AfterClass
    public static void tearDownDB() throws Exception {
        esr.stopLocally();
    }
    

}
