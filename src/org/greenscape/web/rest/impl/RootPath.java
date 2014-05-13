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
import org.osgi.service.log.LogService;

@Path("api")
@Component(service = Object.class)
public class RootPath {
	public List<RestService> restServices = new ArrayList<>();

	private BundleContext context;
	private LogService logService;

	@Path("/model/{name}")
	public Object resource(@PathParam("name") String resourceName) {
		if (restServices.size() == 0) {
			throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("No resource available").build());
		}
		for (RestService service : restServices) {
			if (service.getResourceName().equalsIgnoreCase(resourceName)) {
				RestService highestRankedService = findServiceByHighestRank(service.getResourceName());
				return highestRankedService;
			}
		}
		throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity("Unknown Model name").build());
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void setRestService(RestService restService) {
		restServices.add(restService);
	}

	public void unsetRestService(RestService restService) {
		restServices.remove(restService);
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
							if (context.getService(r).getResourceName().equals(service.getResourceName())) {
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
							if (context.getService(r).getResourceName().equals(service.getResourceName())) {
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
