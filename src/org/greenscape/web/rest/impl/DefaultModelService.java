package org.greenscape.web.rest.impl;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.greenscape.core.ResourceRegistry;
import org.greenscape.core.service.Service;
import org.greenscape.persistence.DocumentModel;
import org.greenscape.persistence.PersistedModelBase;
import org.greenscape.web.rest.AbstractRestService;
import org.greenscape.web.rest.RestService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.log.LogService;

@Component
public class DefaultModelService extends AbstractRestService implements RestService {

	@Override
	public String getResourceName() {
		return "<default>";
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public String addModel(@Context UriInfo uriInfo, Map<String, String> param) {
		DocumentModel entity;
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "ADD");
		try {
			entity = new PersistedModelBase();
			copy(entity, param);
			service.save(resourceName, entity);
			return entity.getModelId();
		} catch (Exception e) {
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
					.build());
		}
	}

	@PUT
	@Path(PARAM_DEF_MODEL_ID)
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateModel(@Context UriInfo uriInfo, @PathParam("modelId") String modelId, Map<String, String> param) {
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "EDIT");
		DocumentModel entity = service.findByModelId(resourceName, modelId);
		if (entity == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("No model with id " + modelId + " exists").build());
		}
		copy(entity, param);
		service.update(resourceName, entity);
	}

	@DELETE
	public void deleteAll(@Context UriInfo uriInfo) {
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "DELETE");
		service.delete(resourceName);
	}

	@DELETE
	@Path(PARAM_DEF_MODEL_ID)
	public void deleteModel(@Context UriInfo uriInfo, @PathParam("modelId") String modelId) {
		String resourceName = uriInfo.getPathParameters().get("name").get(0);
		checkPermission(resourceName + ":1:" + "DELETE");
		service.delete(resourceName, modelId);
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
	}

	@Modified
	public void modified(ComponentContext ctx, Map<String, Object> config) {
	}

	@Override
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void setService(Service service) {
		this.service = service;
	}

	public void unsetService(Service service) {
		this.service = null;
	}

	@Override
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void setResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	public void unsetResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = null;
	}

	@Override
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}

	private void copy(DocumentModel entity, Map<String, String> param) {
		for (String name : param.keySet()) {
			if (param.get(name) != null) {
				entity.setProperty(name, param.get(name));
			}
		}
	}
}
