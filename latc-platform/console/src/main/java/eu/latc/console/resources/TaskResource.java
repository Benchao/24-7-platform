package eu.latc.console.resources;

import java.util.Set;

import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonConverter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.latc.console.MainApplication;
import eu.latc.console.ObjectManager;
import eu.latc.console.objects.Task;

/**
 * The default handler is attached to <api_path>/<id> If the id is "new" and the
 * request a POST, then a new configuration file is created Otherwise, the call
 * is re-directed to <api_path>/<id>/about
 * 
 * @author cgueret
 * 
 */
public class TaskResource extends BaseResource {
	// Logger instance
	protected final Logger logger = LoggerFactory.getLogger(TaskResource.class);

	// The ID of the task
	protected String taskID;

	// The task requested
	protected Task task;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.restlet.resource.UniformResource#doInit()
	 */
	@Override
	protected void doInit() throws ResourceException {
		// Get the "ID" attribute value taken from the URI template /{ID}.
		taskID = (String) getRequest().getAttributes().get("ID");

		// If no ID has been given, return a 404
		if (taskID == null) {
			setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			setExisting(false);
		}

		// Try to get the task
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		try {
			// Try the ID
			task = manager.getTaskByID(taskID);

			// If nothing is found, try again with the slug
			if (task == null)
				task = manager.getTaskBySlug(taskID);

			// Ok, that doesn't exist. Complain and exit
			if (task == null) {
				setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				setExisting(false);
			}
		} catch (Exception e) {
			throw new ResourceException(e);
		}

		// TODO See how to implement content negotiation
		// setNegotiated(true);
		// getVariants().add(new Variant(MediaType.APPLICATION_XML));
		// getVariants().add(new Variant(MediaType.TEXT_XML));
		// getVariants().add(new Variant(MediaType.TEXT_HTML));
	}

	/**
	 * Delete a configuration file
	 * 
	 * @throws Exception
	 */
	@Delete
	public Representation remove(Form parameters) throws Exception {
		// Check credentials
		if (parameters.getFirstValue("api_key", true) == null
				|| !parameters.getFirstValue("api_key", true).equals(APIKey.KEY)) {
			setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return null;
		}

		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();

		// Try to get the configuration file associated to the ID
		manager.deleteTask(taskID);

		return new StringRepresentation("deleted configuration file", MediaType.TEXT_HTML);
	}

	/**
	 * Return information about the linking specification
	 * 
	 * @throws Exception
	 * 
	 */
	@Get
	public Representation getInformation() throws Exception {
		logger.info("[GET] Asked about " + taskID);

		// Get an object manager
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();

		// Get the task
		Task task = manager.getTaskByID(taskID);

		// Prepare the answer
		JSONObject entry = task.toJSON();
		JsonConverter conv = new JsonConverter();
		logger.info("Answer " + entry.toString());

		return conv.toRepresentation(entry, null, null);
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Put
	public Representation updateInformation(Form form) throws Exception {
		logger.info("[PUT] Update details for " + taskID + " with " + form);

		// Check credentials
		if (form.getFirstValue("api_key", true) == null || !form.getFirstValue("api_key", true).equals(APIKey.KEY)) {
			setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return null;
		}

		// Update
		Set<String> keys = form.getNames();
		if (keys.contains("title"))
			task.setTitle(form.getFirstValue("title"));
		if (keys.contains("description"))
			task.setDescription(form.getFirstValue("description"));
		if (keys.contains("author"))
			task.setAuthor(form.getFirstValue("author"));
		if (keys.contains("executable"))
			task.setExecutable(Boolean.parseBoolean(form.getFirstValue("executable")));
		if (keys.contains("vetted"))
			task.setVetted(Boolean.parseBoolean(form.getFirstValue("vetted")));

		// Save the task
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		manager.saveTask(task);

		setStatus(Status.SUCCESS_OK);
		return null;
	}
}
