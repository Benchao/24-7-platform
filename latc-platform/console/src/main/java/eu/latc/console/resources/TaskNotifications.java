package eu.latc.console.resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.atom.Entry;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.atom.Text;
import org.restlet.ext.json.JsonConverter;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.latc.console.MainApplication;
import eu.latc.console.ObjectManager;
import eu.latc.console.objects.Notification;

/**
 * @author cgueret
 * 
 */
public class TaskNotifications extends TaskResource {
	// Logger instance
	protected final Logger logger = LoggerFactory.getLogger(TaskNotifications.class);

	/**
	 * Return the notifications about the task
	 * 
	 * @throws Exception
	 */
	@Get("json")
	public Representation toJSON() throws Exception {
		logger.info("[GET-JSON] Asked notifications for " + taskID);

		JSONArray array = new JSONArray();
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		for (Notification report : manager.getNotificationsForTask(taskID))
			array.put(report.toJSON());

		JSONObject json = new JSONObject();
		json.put("notification", array);
		JsonConverter conv = new JsonConverter();
		logger.info(json.toString());
		return conv.toRepresentation(json, null, null);
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Get("atom")
	public Feed toAtom() throws Exception {
		logger.info("[GET-ATOM] Asked for notifications");

		// Get access to the entity manager stored in the app
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		Feed result = new Feed();
		result.setTitle(new Text("LATC latest notifications"));
		Entry entry;

		for (Notification report : manager.getNotificationsForTask(taskID)) {
			entry = new Entry();
			entry.setTitle(new Text("(" + report.getSeverity() + ")" + report.getMessage()));
			StringBuffer summary = new StringBuffer();
			summary.append("Task:" + report.getTaskTitle()).append("\n");
			summary.append("Date:" + report.getDate()).append("\n");
			summary.append("Extra:" + report.getData()).append("\n");
			entry.setSummary(summary.toString());
			result.getEntries().add(entry);
		}
		return result;
	}

	/**
	 * Adds a new report for this task
	 * 
	 * @throws Exception
	 * 
	 */
	@Post
	public Representation add(Form form) throws Exception {
		// Check credentials
		if (form.getFirstValue("api_key", true) == null
				|| !form.getFirstValue("api_key", true).equals(APIKey.KEY)) {
			setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return null;
		}

		// We need at least a message
		if (form.getFirstValue("message", true) == null) {
			setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return null;
		}

		logger.info("[POST] Add notification " + form.toString() + " for " + taskID);

		// Create a notification report
		Notification notification = new Notification();
		notification.setMessage(form.getFirstValue("message", true));
		notification.setData(form.getFirstValue("data", true));
		notification.setSeverity(form.getFirstValue("severity", true));
		if (notification.getSeverity() == null)
			notification.setSeverity("info");

		// Send it to the object manager
		ObjectManager manager = ((MainApplication) getApplication()).getObjectManager();
		manager.addNotification(taskID, notification);

		setStatus(Status.SUCCESS_OK);
		return new StringRepresentation("Report added", MediaType.TEXT_HTML);
	}
}
