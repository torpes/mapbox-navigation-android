package com.mapbox.services.android.navigation.v5;

/**
 * Generic Exception for all things Mapbox Navigation.
 *
 * @since 0.1.1
 */
public class NavigationException extends RuntimeException {

  /**
   * A form of {@code Throwable} that indicates conditions that a reasonable application might
   * want to catch.
   *
   * @param message the detail message (which is saved for later retrieval by the
   *                {@link #getMessage()} method).
   * @since 0.1.1
   */
  public NavigationException(String message) {
    super(message);
  }
}