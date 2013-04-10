package de.olafklischat.esmapper;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.common.base.Objects;

public class ESRunner {
    //class is NOT safe to be used from multiple threads

    private static final Logger log = Logger.getLogger(ESRunner.class);

    private NodeBuilder localNodeBuilder;
    private Node localNode;
    
    private String workPathOfLatestLocalRun;
    private String logsPathOfLatestLocalRun;
    private String[] dataPathsOfLatestLocalRun;
    
    public ESRunner(int httpPort) {
        localNodeBuilder = NodeBuilder.nodeBuilder().local(false).client(false).data(true);
        Random r = new Random();
        config("node.name", "esrunner_node_" + r.nextInt());
        config("http.port", httpPort);
        config("transport.tcp.port", httpPort + 100);
        setClusterName("esrunner_cluster_" + r.nextInt());
    }
    
    public ESRunner config(String key, Object value) {
        if (isRunningLocally()) {
            throw new IllegalStateException("can't config() a running node.");
        }
        localNodeBuilder.settings().put(key, "" + value);
        return this;
    }
    
    public String getConfig(String key) {
        if (localNode != null) {
            return localNode.settings().get(key);
        } else {
            return localNodeBuilder.settings().get(key);
        }
    }
    
    public ESRunner setClusterName(String name) {
        return config("cluster.name", name);
    }
    
    public String getClusterName() {
        return Objects.firstNonNull(getConfig("cluster.name"), "elasticsearch");
    }
    
    public ESRunner setConfigPath(String path) {
        return config("path.conf", path);
    }
    
    public String getConfigPath() {
        return Objects.firstNonNull(getConfig("path.conf"), "config");
    }
    
    public String[] getDataPaths() {
        return Objects.firstNonNull(getConfig("path.data"), "data").split(",");
    }

    public String getWorkPath() {
        return Objects.firstNonNull(getConfig("path.work"), "work");
    }

    public String getLogsPath() {
        return Objects.firstNonNull(getConfig("path.logs"), "logs");
    }

    public int getHttpPort() {
        return Integer.valueOf(Objects.firstNonNull(getConfig("http.port"), "9200"));
    }

    public void startLocally() {
        if (isRunning()) {
            throw new IllegalStateException("node is already running");
        }
        localNode = localNodeBuilder.node();
        //TODO: handle asynchronous errors?
    }
    
    public void stopLocally() {
        //TODO: try to shutdown ES using Java API, so it works across JVMs too
        //  http://www.elasticsearch.org/guide/reference/api/admin-cluster-nodes-shutdown/
        if (isRunningLocally()) {
            workPathOfLatestLocalRun = getWorkPath();
            logsPathOfLatestLocalRun = getLogsPath();
            dataPathsOfLatestLocalRun = getDataPaths();
            localNode.stop();
            localNode = null;
        }
    }
    
    public void join() {
        while (isRunning()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean isRunningLocally() {
        return localNode != null && ! localNode.isClosed();
    }
    
    /**
     * Also works if the ES instance was started externally (in a different JVM).
     */
    public boolean isRunning() {
        int httpPort = getHttpPort();
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(httpPort);
            return false;
        } catch (BindException be) {
            return true;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (ss != null) {
                    ss.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
    public Node createClient() {
        NodeBuilder clientBuilder = NodeBuilder.nodeBuilder().local(false).client(true).data(false);
        clientBuilder.settings().put("es.path.conf", getConfigPath());
        clientBuilder.settings().put("cluster.name", getClusterName());
        clientBuilder.settings().put("es.node.name", "clientnode");
        return clientBuilder.node();
    }
    
    public void cleanup() {
        if (isRunningLocally()) {
            throw new IllegalStateException("can't cleanup() a running node.");
        }
        List<String> paths = new LinkedList<String>();
        paths.add(Objects.firstNonNull(workPathOfLatestLocalRun, getWorkPath()));
        paths.add(Objects.firstNonNull(logsPathOfLatestLocalRun, getLogsPath()));
        paths.addAll(Arrays.asList(Objects.firstNonNull(dataPathsOfLatestLocalRun, getDataPaths())));
        for (String dp : paths) {
            File dir = new File(dp);
            if (dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    log.error("couldn't delete directory: " + dir + ": " + e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
