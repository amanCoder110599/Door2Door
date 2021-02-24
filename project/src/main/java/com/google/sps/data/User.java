package com.google.sps.data;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public final class User {

  private final long id;
  private final String name;
  private final String email;
  private final String phone;
  private final float rating;

  public User(long id, String name, String email, String phone, float rating) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.rating = rating;
  }

  /**
   * Static function to get a User Object from datastore from corresponding email.
   *
   * @return The User object corresponding to the email, if the user is registered in datastore. If
   *     no such entity is stored in datastore, then it returns null.
   * @param User e-mail id.
   */
  public static User getUserFromEmail(String userEmail) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Filter emailFilter = new FilterPredicate("email", Query.FilterOperator.EQUAL, userEmail);
    Query query = new Query("User").setFilter(emailFilter);
    PreparedQuery preparedQuery = datastore.prepare(query);
    Entity userEntity = preparedQuery.asSingleEntity();

    if (userEntity == null) {
      // There is no user in datastore with this email
      return null;
    }

    long id = userEntity.getKey().getId();
    String name = userEntity.getProperty("name").toString();
    String email = userEntity.getProperty("email").toString();
    String phone = userEntity.getProperty("phone").toString();
    float rating = Float.parseFloat(userEntity.getProperty("rating").toString());
    User user = new User(id, name, email, phone, rating);

    return user;
  }

  /**
   * Static function to get a User Object from datastore from corresponding Datastore id.
   *
   * @return The User object corresponding to the id, if the user is registered in datastore. If no
   *     such entity is stored in datastore, then it returns null.
   * @param User id in datastore
   */
  public static User getUserFromId(long id) {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userEntity;
    try {
      userEntity = datastore.get(KeyFactory.createKey("User", id));
    } catch (Exception e) {
      System.out.println(e);
      return null;
    }

    String name = userEntity.getProperty("name").toString();
    String email = userEntity.getProperty("email").toString();
    String phone = userEntity.getProperty("phone").toString();
    float rating = Float.parseFloat(userEntity.getProperty("rating").toString());
    User user = new User(id, name, email, phone, rating);

    return user;
  }

  public long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public float getRating() {
    return this.rating;
  }

  public String getEmail() {
    return this.email;
  }

  public String getPhone() {
    return this.phone;
  }

  public boolean isProfileComplete() {
    return (this.name.length() > 0);
  }
}
