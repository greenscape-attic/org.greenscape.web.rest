package org.greenscape.web.rest.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.greenscape.core.ModelResource;
import org.greenscape.core.ResourceRegistry;
import org.greenscape.web.rest.RestService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.log.LogService;

@Path("api")
@Component(service = Object.class)
public class RootPath {
	public List<RestService> restServices = new ArrayList<>();
	private ResourceRegistry resourceRegistry;

	private BundleContext context;
	private LogService logService;

	@Path("/model/{name}")
	public Object resource(@PathParam("name") String resourceName) {
		if (restServices.size() == 0) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("No resource available").build());
		}
		ModelResource modelResource = resourceRegistry.getResourceByRemoteName(resourceName);
		if (modelResource == null) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND)
					.entity("Unknown resource name: " + resourceName).build());
		}
		RestService defaultService = null;
		for (RestService service : restServices) {
			if (service.getResourceName().equals("<default>")) {
				defaultService = service;
			}
			if (service.getResourceName().equalsIgnoreCase(resourceName)) {
				RestService highestRankedService = findServiceByHighestRank(service.getResourceName());
				return highestRankedService;
			}
		}
		if (defaultService != null) {
			return defaultService;
		} else {
			// defaultService may not have been assigned, search again
			for (RestService service : restServices) {
				if (service.getResourceName().equals("<default>")) {
					return defaultService;
				}
			}
		}
		throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Unknown Model name").build());
	}

	@Path("/weblet")
	public Object weblet() {
		Subject subject = SecurityUtils.getSubject();
		if (!subject.isAuthenticated()) {
			// throw new
			// WebApplicationException(Response.status(Status.UNAUTHORIZED).entity("Not authenticated").build());
		}
		if (subject.hasRole("super_admin")) {
			System.out.println(">>>>>>>>>>>>>>>>>>");
		}
		for (RestService service : restServices) {
			if (service instanceof WebletService) {
				return service;
			}
		}
		throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Weblet Service not available")
				.build());
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void setRestService(RestService restService) {
		restServices.add(restService);
	}

	public void unsetRestService(RestService restService) {
		restServices.remove(restService);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	public void unsetResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = null;
	}

	private RestService findServiceByHighestRank(String modelName) {
		RestService rankedService = null;
		ServiceReference<RestService> ref = null;
		Collection<ServiceReference<RestService>> references = null;
		try {
			references = context.getServiceReferences(RestService.class, null);

			int rank = 0;
			for (RestService service : restServices) {
				if (service.getResourceName().equalsIgnoreCase(modelName)) {
					if (rankedService == null) {
						rankedService = service;
						for (ServiceReference<RestService> r : references) {
							if (context.getService(r).getClass().equals(service.getClass())) {
								ref = r;
								break;
							}
						}
						if (ref.getProperty(Constants.SERVICE_RANKING) != null) {
							rank = Integer.parseInt((String) ref.getProperty(Constants.SERVICE_RANKING));
						}
					} else {
						ServiceReference<RestService> ref1 = null;
						for (ServiceReference<RestService> r : references) {
							if (context.getService(r).getClass().equals(service.getClass())) {
								ref1 = r;
								break;
							}
						}
						int rank1 = 0;
						if (ref1.getProperty(Constants.SERVICE_RANKING) != null) {
							rank1 = Integer.parseInt((String) ref1.getProperty(Constants.SERVICE_RANKING));
						}
						if (rank1 > rank) {
							rankedService = service;
							ref = ref1;
							rank = rank1;
						}
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
		}
		return rankedService;
	}
}
