package com.walmartlabs.concord.server.api.process;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */


import com.walmartlabs.concord.server.api.IsoDateParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Api(value = "Process Events", authorizations = {@Authorization("api_key"), @Authorization("session_key"), @Authorization("ldap")})
@Path("/api/v1/process")
public interface ProcessEventResource {

    /**
     * Register a process event.
     *
     * @param processInstanceId
     * @param req
     */
    @POST
    @ApiOperation(value = "Register a process event", authorizations = {@Authorization("session_key"), @Authorization("api_key")})
    @Path("/{processInstanceId}/event")
    @Consumes(MediaType.APPLICATION_JSON)
    void event(@ApiParam @PathParam("processInstanceId") UUID processInstanceId,
               @ApiParam ProcessEventRequest req);

    /**
     * List process events.
     *
     * @param processInstanceId
     * @return
     */
    @GET
    @ApiOperation(value = "List process events", responseContainer = "list", response = ProcessEventEntry.class)
    @Path("/{processInstanceId}/event")
    @Produces(MediaType.APPLICATION_JSON)
    List<ProcessEventEntry> list(@ApiParam @PathParam("processInstanceId") UUID processInstanceId,
                                 @ApiParam @QueryParam("after") IsoDateParam geTimestamp,
                                 @ApiParam @QueryParam("limit") @DefaultValue("-1") int limit);
}
