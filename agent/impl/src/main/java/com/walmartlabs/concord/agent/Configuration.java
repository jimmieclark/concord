package com.walmartlabs.concord.agent;

import com.google.common.base.Throwables;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Configuration implements Serializable {

    public static final String SERVER_HOST_KEY = "SERVER_HOST";
    public static final String SERVER_PORT_KEY = "SERVER_PORT";
    public static final String LOG_DIR_KEY = "AGENT_LOG_DIR";
    public static final String PAYLOAD_DIR_KEY = "AGENT_PAYLOAD_DIR";
    public static final String JAVA_CMD_KEY = "AGENT_JAVA_CMD";
    public static final String DEPENDENCY_CACHE_DIR_KEY = "DEPS_CACHE_DIR";
    public static final String RUNNER_PATH = "RUNNER_PATH";
    public static final String WORKERS_COUNT_KEY = "WORKERS_COUNT";

    private final String serverHost;
    private final int serverPort;
    private final Path logDir;
    private final Path payloadDir;
    private final String agentJavaCmd;
    private final Path dependencyCacheDir;
    private final Path runnerPath;
    private final int workersCount;

    public Configuration() {
        try {
            this.serverHost = getEnv(SERVER_HOST_KEY, "localhost");
            this.serverPort = Integer.parseInt(getEnv(SERVER_PORT_KEY, "8101"));

            this.logDir = getDir(LOG_DIR_KEY, "logDir");
            this.payloadDir = getDir(PAYLOAD_DIR_KEY, "payloadDir");
            this.agentJavaCmd = getEnv(JAVA_CMD_KEY, "java");
            this.dependencyCacheDir = getDir(DEPENDENCY_CACHE_DIR_KEY, "depsCacheDir");

            String s = System.getenv(RUNNER_PATH);
            if (s == null) {
                Properties props = new Properties();
                props.load(Configuration.class.getResourceAsStream("runner.properties"));
                s = props.getProperty("runner.path");
            }

            this.runnerPath = Paths.get(s);

            this.workersCount = Integer.parseInt(getEnv(WORKERS_COUNT_KEY, "2"));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public Path getLogDir() {
        return logDir;
    }

    public Path getPayloadDir() {
        return payloadDir;
    }

    public String getAgentJavaCmd() {
        return agentJavaCmd;
    }

    public Path getDependencyCacheDir() {
        return dependencyCacheDir;
    }

    public Path getRunnerPath() {
        return runnerPath;
    }

    public int getWorkersCount() {
        return workersCount;
    }

    private static String getEnv(String key, String defaultValue) {
        String s = System.getenv(key);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }

    private static Path getDir(String key, String defaultPrefix) throws IOException {
        String s = System.getenv(key);
        if (s == null) {
            return Files.createTempDirectory(defaultPrefix);
        }

        Path p = Paths.get(s);
        if (!Files.exists(p)) {
            try {
                Files.createDirectories(p);
            } catch (IOException e) {
                throw new RuntimeException("Can't create a directory: " + p, e);
            }
        }
        return p;
    }
}
