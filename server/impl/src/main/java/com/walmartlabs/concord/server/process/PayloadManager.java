package com.walmartlabs.concord.server.process;

import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.server.api.process.ProcessKind;
import com.walmartlabs.concord.server.process.PayloadParser.EntryPoint;
import com.walmartlabs.concord.server.process.state.ProcessStateManager;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.walmartlabs.concord.server.process.state.ProcessStateManager.copyTo;

@Named
public class PayloadManager {

    private static final String WORKSPACE_DIR_NAME = "workspace";
    private static final String INPUT_ARCHIVE_NAME = "_input.zip";

    private final ProcessStateManager stateManager;

    @Inject
    public PayloadManager(ProcessStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Creates a payload. It is implied that all necessary resources to start a process are
     * supplied in the multipart data and/or provided by a project's repository or a template.
     *
     * @param instanceId
     * @param parentInstanceId
     * @param initiator
     * @param input
     * @return
     */
    public Payload createPayload(UUID instanceId, UUID parentInstanceId, String initiator, EntryPoint entryPoint, MultipartInput input) throws IOException {
        Path baseDir = createPayloadDir();
        Path workspaceDir = ensureWorkspace(baseDir);

        Payload p = PayloadParser.parse(instanceId, parentInstanceId, baseDir, input)
                .putHeader(Payload.BASE_DIR, baseDir)
                .putHeader(Payload.WORKSPACE_DIR, workspaceDir);

        p = addInitiator(p, initiator);
        return addEntryPoint(p, entryPoint);
    }

    /**
     * Creates a payload from the supplied map of parameters.
     *
     * @param instanceId
     * @param parentInstanceId
     * @param initiator
     * @param request
     * @return
     */
    public Payload createPayload(UUID instanceId, UUID parentInstanceId, String initiator, EntryPoint entryPoint, Map<String, Object> request) throws IOException {
        Path baseDir = createPayloadDir();
        Path workspaceDir = ensureWorkspace(baseDir);

        Payload p = new Payload(instanceId, parentInstanceId)
                .putHeader(Payload.BASE_DIR, baseDir)
                .putHeader(Payload.WORKSPACE_DIR, workspaceDir)
                .putHeader(Payload.REQUEST_DATA_MAP, request);

        p = addInitiator(p, initiator);
        return addEntryPoint(p, entryPoint);
    }

    /**
     * Creates a payload from an archive, containing all necessary resources.
     *
     * @param instanceId
     * @param parentInstanceId
     * @param initiator
     * @param in
     * @return
     */
    public Payload createPayload(UUID instanceId, UUID parentInstanceId, String initiator, EntryPoint entryPoint, InputStream in) throws IOException {
        Path baseDir = createPayloadDir();
        Path workspaceDir = ensureWorkspace(baseDir);

        Path archive = baseDir.resolve(INPUT_ARCHIVE_NAME);
        Files.copy(in, archive);

        Payload p = new Payload(instanceId, parentInstanceId);

        p = addInitiator(p, initiator);

        p = p.putHeader(Payload.BASE_DIR, baseDir)
                .putHeader(Payload.WORKSPACE_DIR, workspaceDir)
                .putAttachment(Payload.WORKSPACE_ARCHIVE, archive);

        return addEntryPoint(p, entryPoint);
    }

    /**
     * Creates a payload from an archive, containing all necessary resources.
     *
     * @param instanceId
     * @param parentInstanceId
     * @param initiator
     * @param in
     * @return
     */
    public Payload createPayload(UUID instanceId, UUID parentInstanceId, String initiator, InputStream in) throws IOException {
        Path baseDir = createPayloadDir();
        Path workspaceDir = ensureWorkspace(baseDir);

        Path archive = baseDir.resolve(INPUT_ARCHIVE_NAME);
        Files.copy(in, archive);

        Payload p = new Payload(instanceId, parentInstanceId);

        p = addInitiator(p, initiator);

        return p.putHeader(Payload.BASE_DIR, baseDir)
                .putHeader(Payload.WORKSPACE_DIR, workspaceDir)
                .putAttachment(Payload.WORKSPACE_ARCHIVE, archive);
    }

    /**
     * Creates a payload to resume a suspended process, pulling the necessary data from the state storage.
     *
     * @param instanceId
     * @param eventName
     * @param req
     * @return
     */
    public Payload createResumePayload(UUID instanceId, String eventName, Map<String, Object> req) throws IOException {
        Path tmpDir = Files.createTempDirectory("payload");

        if (!stateManager.export(instanceId, copyTo(tmpDir))) {
            throw new ProcessException(instanceId, "Can't resume '" + instanceId + "', state snapshot not found");
        }

        return new Payload(instanceId)
                .putHeader(Payload.WORKSPACE_DIR, tmpDir)
                .putHeader(Payload.REQUEST_DATA_MAP, req)
                .putHeader(Payload.RESUME_EVENT_NAME, eventName);
    }

    /**
     * Creates a payload to fork an existing process.
     *
     * @param instanceId
     * @param parentInstanceId
     * @param projectName
     * @param req
     * @return
     */
    public Payload createFork(UUID instanceId, UUID parentInstanceId, ProcessKind kind,
                              String initiator, String projectName, Map<String, Object> req) throws IOException {
        Path tmpDir = Files.createTempDirectory("payload");

        if (!stateManager.export(parentInstanceId, copyTo(tmpDir))) {
            throw new ProcessException(instanceId, "Can't fork '" + instanceId + "', parent state snapshot not found");
        }

        Payload p = new Payload(instanceId, parentInstanceId)
                .putHeader(Payload.PROCESS_KIND, kind)
                .putHeader(Payload.WORKSPACE_DIR, tmpDir)
                .putHeader(Payload.PROJECT_NAME, projectName)
                .putHeader(Payload.REQUEST_DATA_MAP, req);

        p = addInitiator(p, initiator);

        return p;
    }

    private Path createPayloadDir() throws IOException {
        return Files.createTempDirectory("payload");
    }

    private Path ensureWorkspace(Path baseDir) throws IOException {
        Path p = baseDir.resolve(WORKSPACE_DIR_NAME);
        if (!Files.exists(p)) {
            Files.createDirectories(p);
        }
        return p;
    }

    private static Payload addInitiator(Payload p, String initiator) {
        if (initiator == null) {
            return p;
        }
        return p.putHeader(Payload.INITIATOR, initiator);
    }

    private static Payload addEntryPoint(Payload p, EntryPoint e) {
        if (e == null) {
            return p;
        }

        // entry point specified in the request has the priority
        String entryPoint = e.getFlow();

        // if it wasn't specified in the request, we should check for
        // the existing entry point value
        if (entryPoint == null) {
            entryPoint = p.getHeader(Payload.ENTRY_POINT);
        }

        // we can also receive the entry point name in the request's
        // JSON data
        if (entryPoint == null) {
            Map<String, Object> req = p.getHeader(Payload.REQUEST_DATA_MAP);
            if (req != null) {
                entryPoint = (String) req.get(Constants.Request.ENTRY_POINT_KEY);
            }
        }

        if (entryPoint != null) {
            p = p.putHeader(Payload.ENTRY_POINT, entryPoint)
                    .mergeValues(Payload.REQUEST_DATA_MAP, Collections.singletonMap(Constants.Request.ENTRY_POINT_KEY, entryPoint));
        }

        return p.putHeader(Payload.PROJECT_NAME, e.getProjectName())
                .putHeader(Payload.REPOSITORY_NAME, e.getRepositoryName());
    }
}
