package org.greenscape.web.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.greenscape.core.service.Service;
import org.greenscape.persistence.DocumentModel;
import org.greenscape.persistence.annotations.Model;
import org.greenscape.web.rest.RestService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;

@Component(name = ModelResource.FACTORY_DS, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModelResource implements RestService {
	static final String FACTORY_DS = "org.greenscape.web.rest.ModelResource.factory";
	private final static String PARAM_DEF_MODEL_ID = "{modelId}";
	private Service service;
	private Class<? extends DocumentModel> clazz;

	private LogService logService;

	@Override
	public String getResourceName() {
		return clazz.getAnnotation(Model.class).name();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<? extends DocumentModel> list(@Context UriInfo uriInfo) {
		List<? extends DocumentModel> list = new ArrayList<>();
		try {
			if (uriInfo.getQueryParameters() == null || uriInfo.getQueryParameters().size() == 0) {
				list = service.find(clazz);
			} else {
				list = service.find(clazz, uriInfo.getQueryParameters());
			}
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(e.getMessage()).build());
		}
		return list;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(PARAM_DEF_MODEL_ID)
	public DocumentModel getModel(@PathParam("modelId") String modelId) {
		DocumentModel model = null;
		model = service.find(clazz, modelId);

		if (model == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("No model with id " + modelId + " exists").build());
		}
		return model;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public String addModel(Map<String, String> param) {
		DocumentModel entity;
		try {
			entity = clazz.newInstance();
			copy(entity, param);
			DocumentModel model = service.save(entity);
			return model.getModelId().toString();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new WebApplicationException(Response.status(Status.NOT_ACCEPTABLE).entity(e.getMessage()).build());
		}
	}

	@PUT
	@Path(PARAM_DEF_MODEL_ID)
	@Consumes(MediaType.APPLICATION_JSON)
	public String updateModel(@PathParam("modelId") String modelId, Map<String, String> param) {
		DocumentModel entity = service.find(clazz, modelId);
		if (entity == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("No model with id " + modelId + " exists").build());
		}
		copy(entity, param);
		service.update(entity);
		return "OK";
	}

	@DELETE
	public String deleteModel() {
		service.delete(clazz);
		return "OK";
	}

	@DELETE
	@Path(PARAM_DEF_MODEL_ID)
	public String deleteModel(@PathParam("modelId") String modelId) {
		service.delete(clazz, modelId);
		return "OK";
	}

	@SuppressWarnings("unchecked")
	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		System.out.println("???????????????-----------");
		try {
			clazz = (Class<? extends DocumentModel>) ctx.getBundleContext().getBundle((Long) config.get("bundleId"))
					.loadClass((String) config.get("modelClass"));
		} catch (NumberFormatException | ClassNotFoundException e) {
			if (logService != null) {
				logService.log(LogService.LOG_ERROR, e.getMessage(), e);
			}
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setService(Service service) {
		this.service = service;
	}

	public void unsetService(Service service) {
		this.service = null;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}

	protected void copy(DocumentModel entity, Map<String, String> param) {
		for (String name : entity.getPropertyNames()) {
			if (param.get(name) != null) {
				entity.setProperty(name, param.get(name));
			}
		}
	}
}
