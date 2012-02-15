package eu.latc.console;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import eu.latc.console.resource.APIKey;
import eu.latc.console.resource.Notifications;
import eu.latc.console.resource.Statistics;
import eu.latc.console.resource.TaskConfiguration;
import eu.latc.console.resource.TaskNotifications;
import eu.latc.console.resource.TaskResource;
import eu.latc.console.resource.TaskTripleSets;
import eu.latc.console.resource.Tasks;

public class MainApplication extends Application {
	// Instance of the manager for configuration files
	private ObjectManager manager = new ObjectManager();

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public Restlet createInboundRoot() {
		// Create a router
		Router router = new Router(getContext());

		// Handler for login
		// GET returns an API key matching a given login/password combination
		router.attach("/api_key", APIKey.class);

		// Handler for the processing queue
		// GET returns the list of tasks
		// POST to create a new task
		router.attach("/tasks", Tasks.class);

		// GET returns the list of all notifications
		router.attach("/notifications", Notifications.class);

		// GET returns a bunch of statistics
		router.attach("/statistics", Statistics.class);

		// Handler for the configuration file associated to the task
		// GET to get the raw XML linking configuration
		// PUT to update the configuration file with a new version
		router.attach("/task/{ID}/configuration", TaskConfiguration.class);

		// Handler for the notifications
		// GET to get a sorted list of reports
		// POST to this address to save a new report
		router.attach("/task/{ID}/notifications", TaskNotifications.class);

		// Handler for the notifications
		// GET to get the content of the triple set named {NAME}
		// PUT to update or create a triple set named {NAME}
		router.attach("/task/{ID}/tripleset/{NAME}", TaskTripleSets.class);

		// Task resource
		// GET to get the description of the task
		// PUT to update the description of the task
		// DELETE to delete the task
		router.attach("/task/{ID}", TaskResource.class);

		// Activate content filtering based on extensions
		getTunnelService().setExtensionsTunnel(true);

		return router;
	}

	/**
	 * @return
	 */
	public ObjectManager getObjectManager() {
		return manager;
	}
}
