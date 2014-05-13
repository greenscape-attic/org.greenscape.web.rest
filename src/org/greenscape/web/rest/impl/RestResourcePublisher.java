package org.greenscape.web.rest.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.greenscape.core.ModelRegistryEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;

@Component
public class RestResourcePublisher {

	private final Map<String, Configuration> configMap = new HashMap<>();
	private final List<ModelRegistryEntry> modelList = new ArrayList<>();

	private BundleContext context;
	private LogService logService;

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
		setupServices();
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void setModelRegistryEntry(ModelRegistryEntry entry) {
		modelList.add(entry);
		setupServices();
	}

	public void unsetModelRegistryEntry(ModelRegistryEntry entry) {
		modelList.remove(entry);
		try {
			configMap.get(entry.getModelClass()).delete();
		} catch (IOException e) {
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
		}
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}

	private void setupServices() {
		if (context == null) {
			return;
		}
		for (ModelRegistryEntry entry : modelList) {
			if (configMap.get(entry.getModelClass()) == null) {
				ServiceReference<ConfigurationAdmin> configAdminRef = context
						.getServiceReference(ConfigurationAdmin.class);
				ConfigurationAdmin confAdmin = context.getService(configAdminRef);
				Dictionary<String, Object> properties = new Hashtable<>();
				properties.put("modelClass", entry.getModelClass());
				properties.put("bundleId", entry.getBundleId());
				Configuration configuration = null;
				try {
					configuration = confAdmin.createFactoryConfiguration(ModelResource.FACTORY_DS, null);
					configMap.put(entry.getModelClass(), configuration);
					configuration.update(properties);
				} catch (Exception e) {
					e.printStackTrace();
					logService.log(LogService.LOG_ERROR, e.getMessage(), e);
				}
			}
		}
	}
}
