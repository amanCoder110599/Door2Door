package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.Task;
import com.google.sps.data.User;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating creation of tasks. */
@WebServlet("/task/create")
public class TaskCreateServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    UserService userService = UserServiceFactory.getUserService();

    // If not logged in, redirect to landing page
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect("/");
      return;
    }

    // Get Logged-in User details
    User loggedInUser = User.getUserFromEmail(userService.getCurrentUser().getEmail());
    if ((loggedInUser == null) || (!loggedInUser.isProfileComplete())) {
      // User is not added in datastore or has not completed profile, redirect to landing page
      response.sendRedirect("/");
      return;
    }

    // Dispatch request to task creation
    request.setAttribute("userLogoutUrl", userService.createLogoutURL("/"));
    request.setAttribute("loggedInUser", loggedInUser);
    request.getRequestDispatcher("/WEB-INF/jsp/task-create.jsp").forward(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();

    // If not logged in, do not create new task
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect("/index.html");
      return;
    }

    // Get Logged-in User details
    User loggedInUser = User.getUserFromEmail(userService.getCurrentUser().getEmail());
    if (loggedInUser == null) {
      // User is not added in datastore, redirect to home page for registering in datastore
      response.sendRedirect("/index.html");
      return;
    }

    // TODO: add checks to task parameters coming from client

    // Form Entity
    Entity taskEntity = new Entity("Task");
    Task task =
        new Task(
            taskEntity.getKey().getId() /* Here Id is 0, as Entity is not yet put in Datastore */,
            getParameter(request, "title", ""),
            getParameter(request, "details", ""),
            Long.parseLong(getParameter(request, "compensation", "0")),
            loggedInUser.getId(),
            getDateTimeLocalAsMillis(
                getParameter(request, "deadline", ""),
                Long.parseLong(getParameter(request, "clientTzOffsetInMins", "0"))),
            getParameter(request, "address", ""));
    if (!setTaskEntityProperties(task, taskEntity)) {
      return;
    }

    // Store in Datastore
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    long taskId = datastore.put(taskEntity).getId();

    // Redirect to Task View to view the created task
    response.sendRedirect("/task/view/" + String.valueOf(taskId));
  }

  /**
   * If present, get the request parameter identified by name, else return defaultValue.
   *
   * @param request The HTTP Servlet Request.
   * @param name The name of the rquest parameter.
   * @param defaultValue The default value to be returned if required parameter is unspecified.
   * @return The request parameter, or the default value if the parameter was not specified by the
   *     client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Convert datetime string from YYYY-MM-DDTHH:MM to number of milliseconds since unix epoch.
   *
   * @param dateTimeString The string of format YYYY-MM-DDTHH:MM.
   * @param timezoneOffsetInMins The timezone offset in mins from UTC.
   * @return The datetime expressed as the number of milliseconds since unix epoch.
   */
  private long getDateTimeLocalAsMillis(String dateTimeString, long timezoneOffsetInMins) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    long milliseconds;

    try {
      Date date = format.parse(dateTimeString);
      milliseconds = date.getTime() + 60000 * timezoneOffsetInMins;
    } catch (ParseException e) {
      e.printStackTrace();
      return -1;
    }

    return milliseconds;
  }

  /**
   * Sets the properties of Task Entity from the corresponding Task Object.
   *
   * @param task The task object.
   * @param taskEntity The Entity of kind Task where details are to be updated.
   * @return True on succesful update. False, otherwise.
   */
  private boolean setTaskEntityProperties(Task task, Entity taskEntity) {
    if ((!taskEntity.getKind().equals("Task")) || (taskEntity.getKey().getId() != task.getId())) {
      return false;
    }

    taskEntity.setProperty("title", task.getTitle());
    taskEntity.setProperty("details", task.getDetails());
    taskEntity.setProperty("creationTime", task.getCreationTime());
    taskEntity.setProperty("compensation", task.getCompensation());
    taskEntity.setProperty("creatorId", task.getCreatorId());
    taskEntity.setProperty("deadline", task.getDeadlineAsLong());
    taskEntity.setProperty("address", task.getAddress());
    taskEntity.setProperty("assigned", task.isAssigned());
    taskEntity.setProperty("assigneeId", task.getAssigneeId());
    taskEntity.setProperty("completionRating", task.getCompletionRating());
    taskEntity.setProperty("active", task.isActive());
    return true;
  }
}
