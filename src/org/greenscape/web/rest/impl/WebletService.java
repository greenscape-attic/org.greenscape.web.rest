package org.greenscape.web.rest.impl;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.greenscape.core.ResourceRegistry;
import org.greenscape.core.WebletResource;
import org.greenscape.web.rest.RestService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;

@Component
public class WebletService implements RestService {
	final static String RESOURCE_NAME = "Weblet";
	private final static String PARAM_DEF_WEBLET_ID = "{webletId}";

	private ResourceRegistry resourceRegistry;

	private BundleContext context;
	private LogService logService;

	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebletResource> list(@Context UriInfo uriInfo) {
		List<WebletResource> resources = resourceRegistry.getResources(WebletResource.class);
		return resources;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(PARAM_DEF_WEBLET_ID)
	public WebletResource getWeblet(@PathParam("webletId") String webletId) {
		List<WebletResource> weblets = resourceRegistry.getResources(WebletResource.class);
		for (WebletResource resource : weblets) {
			if (resource.getId().equals(webletId)) {
				return resource;
			}
		}
		throw new WebApplicationException(Response.status(Status.NOT_FOUND)
				.entity("No weblet with id " + webletId + " exists").build());
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	public void unsetResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = null;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}
}
