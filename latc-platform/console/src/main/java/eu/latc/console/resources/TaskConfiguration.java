package eu.latc.console.resources;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.latc.console.MainApplication;
import eu.latc.console.ObjectManager;
import eu.latc.console.objects.Notification;

/**
 * @author cgueret
 * 
 */
public class TaskConfiguration extends TaskResource {
	// Logger instance
	protected final Logger logger = LoggerFactory.getLogger(TaskConfiguration.class);

	/**
	 * Update an existing configuration file
	 * 
	 * @param form
	 *            the configuration file content to put under the identifier
	 * @throws Exception
	 */
	@Put
	public Representation update(Form form) throws Exception {
		// Parse the identifier
		logger.info("[PUT] Update configuration file for " + taskID + " with " + form.toString());

		// Check credentials
		if (form.getFirstValue("api_key", true) == null
				|| !form.getFirstValue("api_key", true).equals(APIKey.KEY)) {
			setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return null;
		}

		// Get the configuration file to assign
		String text = form.getFirstValue("configuration");
		if (text == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}

		// Update the value and persist the task
		task.setConfiguration(text);
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		manager.saveTask(task);

		// Add a notification
		Notification notification = new Notification();
		notification.setSeverity("warn");
		notification.setMessage("Configuration modified");
		manager.addNotification(taskID, notification);

		setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("updated", MediaType.TEXT_HTML);
	}

	/**
	 * Return a the XML configuration of the task
	 * 
	 */
	@Override
	@Get
	public Representation get() {
		try {
			// A specific configuration file has been asked
			logger.info("[GET] Return the XML linking configuration of " + taskID);
			return new DomRepresentation(MediaType.TEXT_XML, task.getDocument());
		} catch (Exception e) {
			e.printStackTrace();

			// If anything goes wrong, just report back on an internal error
			setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		}
	}

}
