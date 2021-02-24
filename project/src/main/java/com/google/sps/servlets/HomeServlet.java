package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet facilitating creation of tasks. */
@WebServlet("/")
public class HomeServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    UserService userService = UserServiceFactory.getUserService();

    if (!userService.isUserLoggedIn()) {
      // Not logged in
      request.setAttribute("isLoggedIn", false);
      request.setAttribute("userLoginUrl", userService.createLoginURL("/"));
    } else {
      // Get Logged-in User details
      String userEmail = userService.getCurrentUser().getEmail();
      User loggedInUser = User.getUserFromEmail(userEmail);

      if (loggedInUser == null) {
        // User with given email does not exist in Datastore,
        // add a User with dummy user details to datastore
        Entity userEntity = new Entity("User");
        userEntity.setProperty("name", "");
        userEntity.setProperty("email", userEmail);
        userEntity.setProperty("phone", "");
        userEntity.setProperty("rating", 0.0);
        datastore.put(userEntity);
        response.sendRedirect("/profile/edit");
        return;
      }

      if (!loggedInUser.isProfileComplete()) {
        // The current user already exists but has dummy details
        response.sendRedirect("/profile/edit");
        return;
      }

      // If the current user is fully registered
      request.setAttribute("isLoggedIn", true);
      request.setAttribute("userLogoutUrl", userService.createLogoutURL("/"));
    }

    // Dispatch request to task creation
    request.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(request, response);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }
}
