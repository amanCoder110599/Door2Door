package com.google.sps.servlets;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.sps.data.Task;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating listing of tasks. */
@WebServlet(urlPatterns = {"/task/all", "/task/created", "/task/assigned", "/task/completed"})
public class TaskListServlet extends HttpServlet {
  static final int PAGE_SIZE = 25;

  /*denotes the approx time in milliseconds which takes the datastore fetch
  results after applying the filter and returning response to the client*/
  static final int OFF_SET = 100;

  private final DatastoreService datastore;

  public TaskListServlet() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String uriInfo = request.getRequestURI();
    UserService userService = UserServiceFactory.getUserService();

    if (!userService.isUserLoggedIn()) {
      return;
    }

    // Define the Query
    Query query = new Query("Task");
    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(PAGE_SIZE);

    // Add required sort
    String sortOptionString = getParameter(request, "sortOption", "Deadline");
    String sortDirectionString = getParameter(request, "sortDirection", "Descending");
    Long userId = -1L;
    if (!uriInfo.equals("/task/all")) userId = getParameter(request, "userId", -1L);

    SortDirection sortDirection;
    if (sortDirectionString.equals("Ascending")) {
      sortDirection = SortDirection.ASCENDING;
    } else {
      sortDirection = SortDirection.DESCENDING;
    }

    // If this servlet is passed a cursor parameter, use it.
    String startCursor = request.getParameter("cursor");
    if (startCursor != null) {
      fetchOptions.startCursor(Cursor.fromWebSafeString(startCursor));
    }

    if (sortOptionString.equals("Creation")) {
      query.addSort("creationTime", sortDirection);
    } else if (sortOptionString.equals("Compensation")) {
      query.addSort("compensation", sortDirection);
    } else {
      query.addSort("deadline", sortDirection);
    }

    // Add active filters
    boolean filterActive = getParameter(request, "filterActive", false);
    if (uriInfo.equals("/task/all")) {
      // assigned = false and deadline has not passed
      if (filterActive) {
        Filter isAssignedFilter =
            new FilterPredicate("assigned", Query.FilterOperator.EQUAL, false);
        query.setFilter(isAssignedFilter);
      }
    } else if (uriInfo.equals("/task/created")) {
      // set filters for created tasks
      Filter uriFilter = new FilterPredicate("creatorId", FilterOperator.EQUAL, userId);
      query.setFilter(uriFilter);
    } else if (uriInfo.equals("/task/assigned")) {
      // set filters for assigned tasks
      Filter uriFilter = new FilterPredicate("assigneeId", FilterOperator.EQUAL, userId);
      Filter activeFilter = new FilterPredicate("active", FilterOperator.EQUAL, true);
      Filter compositeFilter =
          new CompositeFilter(
              CompositeFilterOperator.AND, Arrays.<Filter>asList(uriFilter, activeFilter));
      query.setFilter(compositeFilter);
    } else {
      // set filters for completed tasks
      Filter uriFilter = new FilterPredicate("assigneeId", FilterOperator.EQUAL, userId);
      Filter activeFilter = new FilterPredicate("active", FilterOperator.EQUAL, false);
      Filter compositeFilter =
          new CompositeFilter(
              CompositeFilterOperator.AND, Arrays.<Filter>asList(uriFilter, activeFilter));
      query.setFilter(compositeFilter);
    }

    // Query the Datastore for the required tasks
    QueryResultList<Entity> results;
    try {
      results = datastore.prepare(query).asQueryResultList(fetchOptions);
    } catch (IllegalArgumentException e) {
      // Illegal Cursor
      return;
    }

    List<Task> tasks = new ArrayList<>();
    for (Entity entity : results) {
      Task task = Task.getTaskFromDatastoreEntity(entity);

      // If deadline has passed and the active filter is not applied on the home page
      if (task.getDeadlineAsLong() < System.currentTimeMillis() + OFF_SET && filterActive) continue;
      tasks.add(task);
    }

    // Send the JSON as the response
    JsonObject json = new JsonObject();
    Gson gson = new Gson();
    json.addProperty("nextCursor", results.getCursor().toWebSafeString());
    json.add("tasks", gson.toJsonTree(tasks));
    response.setContentType("application/json;");
    response.getWriter().println(json.toString());
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
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

  private boolean getParameter(HttpServletRequest request, String name, boolean defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  private Long getParameter(HttpServletRequest request, String name, Long defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return Long.parseLong(value);
  }
}
