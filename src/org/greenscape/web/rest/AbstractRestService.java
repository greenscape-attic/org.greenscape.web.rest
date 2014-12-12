package org.greenscape.web.rest;

import java.util.ArrayList;
import java.util.List;

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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.greenscape.core.ResourceRegistry;
import org.greenscape.core.service.Service;
import org.greenscape.persistence.DocumentModel;
import org.osgi.service.log.LogService;

public abstract class AbstractRestService implements RestService {
	public final static String PARAM_DEF_MODEL_ID = "{modelId}";
	protected Service service;
	protected ResourceRegistry resourceRegistry;
	protected LogService logService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<? extends DocumentModel> list(@Context UriInfo uriInfo) {
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "VIEW");
		List<? extends DocumentModel> list = new ArrayList<>();
		try {
			if (uriInfo.getQueryParameters() == null || uriInfo.getQueryParameters().size() == 0) {
				list = service.find(resourceName);
			} else {
				list = service.find(resourceName, uriInfo.getQueryParameters());
			}
		} catch (IllegalArgumentException e) {
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(e.getMessage()).build());
		}
		return list;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(PARAM_DEF_MODEL_ID)
	public DocumentModel getModel(@Context UriInfo uriInfo, @PathParam("modelId") String modelId) {
		DocumentModel model = null;
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "VIEW");
		model = service.findByModelId(resourceName, modelId);

		if (model == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("No model with id " + modelId + " exists").build());
		}
		return model;
	}

	public abstract void setService(Service service);

	public abstract void setResourceRegistry(ResourceRegistry resourceRegistry);

	public abstract void setLogService(LogService logService);

	@Override
	public String toString() {
		return getResourceName();
	}

	protected void checkPermission(String permission) {
		Subject subject = SecurityUtils.getSubject();
		if (!subject.isAuthenticated()) {
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity("Unauthenticated access")
					.build());
		}
		if (!subject.isPermitted(permission)) {
			throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).entity("Not authorized").build());
		}
	}

	protected void checkPermission(String resourceName, int scope, String action) {
		checkPermission(resourceName + ":" + scope + ":" + action);
	}

}
