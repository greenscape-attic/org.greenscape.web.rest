package org.greenscape.web.rest.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.greenscape.core.ModelResource;
import org.greenscape.core.Resource;
import org.greenscape.core.ResourceEvent;
import org.greenscape.core.ResourceRegistry;
import org.greenscape.core.ResourceType;
import org.greenscape.web.rest.RestConstants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

@Deprecated
// @Component(property = { EventConstants.EVENT_TOPIC + "=" +
// ResourceRegistry.TOPIC_RESOURCE_REGISTERED, EventConstants.EVENT_TOPIC + "="
// + ResourceRegistry.TOPIC_RESOURCE_UNREGISTERED })
public class RestResourcePublisher implements EventHandler {

	private ResourceRegistry resourceRegistry;
	private final List<Configuration> configurations;

	private ConfigurationAdmin configurationAdmin;
	private LogService logService;

	public RestResourcePublisher() {
		configurations = new ArrayList<Configuration>();
	}

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		setupServices();
	}

	@Modified
	public void modified(ComponentContext ctx, Map<String, Object> config) {

	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case ResourceRegistry.TOPIC_RESOURCE_REGISTERED:
			setupServices();
			break;
		case ResourceRegistry.TOPIC_RESOURCE_UNREGISTERED:
			uninstallService(event);
			break;
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	public void unsetResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = null;
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

	public void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = null;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}

	private void setupServices() {
		try {
			List<Resource> resources = resourceRegistry.getResources(ResourceType.Model);
			for (Resource entry : resources) {
				ModelResource modelResource = (ModelResource) entry;
				if (!modelResource.isAbstract() && modelResource.isRemote() && !isConfigPresent(entry)) {
					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put("modelName", entry.getName());
					properties.put("bundleId", entry.getBundleId());
					Configuration configuration = null;
					try {
						configuration = configurationAdmin.createFactoryConfiguration(
								RestConstants.MODEL_SERVICE_FACTORY_DS, null);
						configuration.update(properties);
					} catch (IOException e) {
						e.printStackTrace();
						logService.log(LogService.LOG_ERROR, e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
		}
	}

	private void uninstallService(Event event) {
		for (Configuration config : configurations) {
			Dictionary<String, Object> properties = config.getProperties();
			String modelName = (String) properties.get("modelName");
			if (modelName.equals(event.getProperty(ResourceEvent.RESOURCE_NAME))) {
				try {
					config.delete();
				} catch (IOException e) {
					logService.log(LogService.LOG_ERROR, "Failed to uninstall REST Service for model " + modelName, e);
				}
			}
		}
	}

	private boolean isConfigPresent(Resource entry) {
		if (configurations != null) {
			for (Configuration config : configurations) {
				Dictionary<String, Object> properties = config.getProperties();
				String modelName = (String) properties.get("modelName");
				if (modelName.equals(entry.getName())) {
					return true;
				}
			}
		}
		return false;
	}
}
