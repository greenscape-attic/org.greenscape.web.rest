package org.greenscape.web.rest.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.greenscape.persistence.ModelRegistryEntry;
import org.greenscape.web.rest.RestConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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
public class RestResourcePublisher {

	private final List<ModelRegistryEntry> modelList = new ArrayList<>();

	private LogService logService;
	private BundleContext context;

	@Activate
	public void activate(ComponentContext ctx, Map<String, Object> config) {
		context = ctx.getBundleContext();
		setupServices(context);
	}

	@Modified
	public void modified(ComponentContext ctx, Map<String, Object> config) {
		setupServices(context);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void setModelRegistryEntry(ModelRegistryEntry entry) {
		modelList.add(entry);
		setupServices(context);
	}

	public void unsetModelRegistryEntry(ModelRegistryEntry entry) {
		modelList.remove(entry);
		// TODO:remove the config entry
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}

	private void setupServices(BundleContext context) {
		if (context == null) {
			return;
		}
		ServiceReference<ConfigurationAdmin> configAdminRef = context.getServiceReference(ConfigurationAdmin.class);
		ConfigurationAdmin confAdmin = context.getService(configAdminRef);
		Configuration[] configurations;
		try {

			for (ModelRegistryEntry entry : modelList) {
				configurations = confAdmin.listConfigurations("(service.factoryPid="
						+ RestConstants.MODEL_RESOURCE_FACTORY_DS + ")");

				if (!isConfigPresent(configurations, entry)) {
					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put("modelClass", entry.getModelClass());
					properties.put("bundleId", entry.getBundleId());
					Configuration configuration = null;
					try {
						configuration = confAdmin.createFactoryConfiguration(RestConstants.MODEL_RESOURCE_FACTORY_DS,
								null);
						configuration.update(properties);
					} catch (Exception e) {
						e.printStackTrace();
						logService.log(LogService.LOG_ERROR, e.getMessage(), e);
					}
				}
			}
		} catch (IOException | InvalidSyntaxException e) {
			e.printStackTrace();
			logService.log(LogService.LOG_ERROR, e.getMessage(), e);
		}
	}

	private boolean isConfigPresent(Configuration[] configurations, ModelRegistryEntry entry) {
		if (configurations != null) {
			for (Configuration config : configurations) {
				Dictionary<String, Object> properties = config.getProperties();
				String modelClass = (String) properties.get("modelClass");
				if (modelClass.equals(entry.getModelClass())) {
					return true;
				}
			}
		}
		return false;
	}
}
