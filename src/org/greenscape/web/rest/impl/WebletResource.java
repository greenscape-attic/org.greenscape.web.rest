package org.greenscape.web.rest.impl;

import java.util.ArrayList;
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

import org.greenscape.core.WebletItem;
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
public class WebletResource implements RestService {
	final static String RESOURCE_NAME = "Weblet";
	private final static String PARAM_DEF_WEBLET_ID = "{webletId}";

	private final List<WebletItem> weblets = new ArrayList<WebletItem>();

	private BundleContext context;
	private LogService logService;

	@Override
	public String getResourceName() {
		return RESOURCE_NAME;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<WebletItem> list(@Context UriInfo uriInfo) {
		return weblets;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(PARAM_DEF_WEBLET_ID)
	public WebletItem getWeblet(@PathParam("webletId") String webletId) {
		for (WebletItem weblet : weblets) {
			if (weblet.getId().equals(webletId)) {
				return weblet;
			}
		}
		throw new WebApplicationException(Response.status(Status.NOT_FOUND)
				.entity("No weblet with id " + webletId + " exists").build());
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void setWeblet(WebletItem weblet) {
		weblets.add(weblet);
	}

	public void unsetWeblet(WebletItem weblet) {
		weblets.remove(weblet);
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}
}
