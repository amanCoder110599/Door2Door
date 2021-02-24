package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating assign task. */
@WebServlet("/task/assign")
public class TaskAssignServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doPost(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    long taskId = getParameter(request, "taskId", -1);
    if (taskId == -1) return;

    long assigneeId = getParameter(request, "assigneeId", -1);

    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      return;
    }

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity taskEntity;
    try {
      taskEntity = datastore.get(KeyFactory.createKey("Task", taskId));
    } catch (Exception e) {
      System.out.println(e);
      return;
    }

    taskEntity.setProperty("assigneeId", assigneeId);
    taskEntity.setProperty("assigned", true);

    datastore.put(taskEntity);

    response.sendRedirect("/task/view/" + String.valueOf(taskId));
  }

  private long getParameter(HttpServletRequest request, String name, long defaultValue) {
    Long value = Long.parseLong(request.getParameter(name));
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
