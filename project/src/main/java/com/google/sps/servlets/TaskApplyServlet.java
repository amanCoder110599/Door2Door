package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating viewing task details. */
@WebServlet("/task/apply")
public class TaskApplyServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Long taskId = getParameter(request, "taskId", -1);
    if (taskId == -1) return;

    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      return;
    }
    String userEmail = userService.getCurrentUser().getEmail();

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Filter emailFilter = new FilterPredicate("email", Query.FilterOperator.EQUAL, userEmail);
    Query query = new Query("User").setFilter(emailFilter);
    PreparedQuery pq = datastore.prepare(query);
    Entity userEntity = pq.asSingleEntity();

    long userId = userEntity.getKey().getId();

    Entity taskApplicantsEntity = new Entity("TaskApplicants");
    taskApplicantsEntity.setProperty("taskId", taskId);
    taskApplicantsEntity.setProperty("applicantId", userId);

    datastore.put(taskApplicantsEntity);

    response.sendRedirect("/task/view/" + taskId.toString());
  }

  private Long getParameter(HttpServletRequest request, String name, long defaultValue) {
    Long value = Long.parseLong(request.getParameter(name));
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
