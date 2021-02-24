package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.Task;
import com.google.sps.data.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating viewing task details. */
@WebServlet(urlPatterns = {"/task/view/*"})
public class TaskViewServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
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

    Long taskId = getTaskIdFromURI(request.getRequestURI());
    if (taskId == null) {
      response.getWriter().println("This task Id is invalid");
      return;
    }

    // Get Task Object from ID
    Entity taskEntity;
    try {
      taskEntity = datastore.get(KeyFactory.createKey("Task", taskId));
    } catch (Exception e) {
      System.out.println(e);
      return;
    }
    Task task = Task.getTaskFromDatastoreEntity(taskEntity);

    if (new Date(task.getDeadlineAsLong()).before(new Date())) {
      // If task is unassigned and past deadline and still active,
      // deactivate it and update datastore
      if ((!task.isAssigned()) && (task.isActive())) {
        task.deactivate();
        taskEntity.setProperty("active", false);
        datastore.put(taskEntity);
      }
    }

    if (task.getCreatorId() == loggedInUser.getId()) {
      // Logged in user is the creator of the task
      request.setAttribute("isCreator", true);
      request.setAttribute("creator", loggedInUser);

      // Prepare the list of applicants
      List<User> taskApplicantList = new ArrayList<>();
      Filter taskIdFilter = new FilterPredicate("taskId", Query.FilterOperator.EQUAL, task.getId());
      Query applicantsQuery = new Query("TaskApplicants").setFilter(taskIdFilter);
      PreparedQuery preparedQuery = datastore.prepare(applicantsQuery);

      for (Entity applicantEntity : preparedQuery.asIterable()) {
        long applicantId = Long.parseLong(applicantEntity.getProperty("applicantId").toString());
        User applicant = User.getUserFromId(applicantId);
        taskApplicantList.add(applicant);
      }
      request.setAttribute("taskApplicantList", taskApplicantList);
    } else {
      // Logged in user is not the creator of the task
      request.setAttribute("isCreator", false);

      // Get creator details
      User creator = User.getUserFromId(task.getCreatorId());
      request.setAttribute("creator", creator);

      Filter taskIdFilter = new FilterPredicate("taskId", Query.FilterOperator.EQUAL, task.getId());
      Filter applicantIdFilter =
          new FilterPredicate("applicantId", Query.FilterOperator.EQUAL, loggedInUser.getId());
      Filter compositeFilter =
          new CompositeFilter(
              CompositeFilterOperator.AND, Arrays.<Filter>asList(taskIdFilter, applicantIdFilter));

      Query appliedQuery = new Query("TaskApplicants").setFilter(compositeFilter);
      PreparedQuery preparedQuery = datastore.prepare(appliedQuery);

      Entity applicantEntity = preparedQuery.asSingleEntity();
      if (applicantEntity == null) {
        // The logged in user has not applied for the task
        request.setAttribute("hasApplied", false);
      } else {
        // The logged in user has applied for the task
        request.setAttribute("hasApplied", true);
      }
    }

    // Get assignee details if any
    if (task.isAssigned()) {
      User assignee = User.getUserFromId(task.getAssigneeId());
      request.setAttribute("assignee", assignee);
    }

    request.setAttribute("task", task);
    request.setAttribute("userLogoutUrl", userService.createLogoutURL("/"));

    // Dispatch request to Task View
    request.getRequestDispatcher("/WEB-INF/jsp/task-view.jsp").forward(request, response);
  }

  /**
   * Returns the Task Id from the URI.
   *
   * @return The task ID, if a valid Id is there in the URI, else null.
   * @param uri The request URI.
   */
  private Long getTaskIdFromURI(String uri) {
    Long taskId;

    try {
      // Task ID starts from the string's 11th index
      if (uri.charAt(uri.length() - 1) == '/') {
        taskId = Long.parseLong(uri.substring(11, uri.length() - 1));
      } else {
        taskId = Long.parseLong(uri.substring(11));
      }
    } catch (NumberFormatException nfe) {
      // Given URI does not contain parsable task ID
      return null;
    }

    return taskId;
  }

  /**
   * If present, get the request parameter identified by name, else return defaultValue.
   *
   * @return The request parameter, or the default value if the parameter was not specified by the
   *     client
   * @param request The HTTP Servlet Request.
   * @param name The name of the rquest parameter.
   * @param defaultValue The default value to be returned if required parameter is unspecified.
   */
  private Long getParameter(HttpServletRequest request, String name, Long defaultValue) {
    Long value = Long.parseLong(request.getParameter(name));
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
