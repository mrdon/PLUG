package com.atlassian.plugin.servlet.filter;

/**
 * An enumeration defining the places plugin filters can appear in an applications filter stack.  Filters in the "top"
 * location will be passed through first, followed by filters in the "middle" and then filters in the "bottom". 
 * Where these locations appear relative to other filters in the application is dependent on the application. 
 */
public enum FilterLocation {
    top, middle, bottom
}