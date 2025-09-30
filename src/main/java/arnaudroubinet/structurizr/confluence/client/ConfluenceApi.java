package arnaudroubinet.structurizr.confluence.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/wiki")
@RegisterRestClient(configKey = "confluence-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ConfluenceApi {

    @GET
    @Path("/rest/api/content")
    Uni<String> findContentByTitle(@QueryParam("title") String title,
                                   @QueryParam("spaceKey") String spaceKey);

    @GET
    @Path("/api/v2/spaces")
    Uni<String> listSpacesByKeys(@QueryParam("keys") String keys);

    @GET
    @Path("/api/v2/spaces/{spaceId}/pages")
    Uni<String> listPages(@PathParam("spaceId") String spaceId, @QueryParam("limit") int limit);

    @GET
    @Path("/api/v2/pages/{pageId}")
    Uni<String> getPage(@PathParam("pageId") String pageId,
                        @QueryParam("body-format") String bodyFormat);

    @GET
    @Path("/api/v2/pages/{pageId}")
    Uni<String> getPageInfo(@PathParam("pageId") String pageId);

    @POST
    @Path("/api/v2/pages")
    Uni<String> createPage(String body);

    @PUT
    @Path("/api/v2/pages/{pageId}")
    Uni<String> updatePage(@PathParam("pageId") String pageId, String body);

    @DELETE
    @Path("/api/v2/pages/{pageId}")
    Uni<Void> deletePage(@PathParam("pageId") String pageId);

    @GET
    @Path("/rest/api/content/{pageId}/child/page")
    Uni<String> listChildPages(@PathParam("pageId") String pageId, @QueryParam("limit") int limit);

    @POST
    @Path("/rest/api/content/{pageId}/child/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Uni<String> uploadAttachment(@PathParam("pageId") String pageId, @org.jboss.resteasy.reactive.PartType(MediaType.APPLICATION_OCTET_STREAM) byte[] file,
                                 @QueryParam("filename") String filename);

    @GET
    @Path("/rest/api/content/{attachmentId}")
    Uni<String> getAttachmentWithExtensions(@PathParam("attachmentId") String attachmentId,
                                            @QueryParam("expand") String expand);
}
