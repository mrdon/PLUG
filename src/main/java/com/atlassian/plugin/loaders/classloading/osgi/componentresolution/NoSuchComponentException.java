package com.atlassian.plugin.loaders.classloading.osgi.componentresolution;

public class NoSuchComponentException extends Exception
{
    /** Name of the missing component */
	private String componentName;

	/** Required component type */
	private Class componentType;


	/**
	 * Create a new NoSuchComponentException.
	 * @param name the name of the missing component
	 */
	public NoSuchComponentException(String name) {
		super("No component named '" + name + "' is defined");
		this.componentName = name;
	}

	/**
	 * Create a new NoSuchComponentException.
	 * @param name the name of the missing component
	 * @param message detailed message describing the problem
	 */
	public NoSuchComponentException(String name, String message) {
		super("No component named '" + name + "' is defined: " + message);
		this.componentName = name;
	}

	/**
	 * Create a new NoSuchComponentException.
	 * @param type required type of component
	 * @param message detailed message describing the problem
	 */
	public NoSuchComponentException(Class type, String message) {
		super("No unique component of type [" + type.getName() + "] is defined: " + message);
		this.componentType = type;
	}

	/**
	 * Create a new NoSuchComponentException.
	 * @param type required type of component
	 * @param dependencyDescription a description of the originating dependency
	 * @param message detailed message describing the problem
	 */
	public NoSuchComponentException(Class type, String dependencyDescription, String message) {
		super("No matching component of type [" + type.getName() + "] found for dependency [" +
				dependencyDescription + "]: " + message);
		this.componentType = type;
	}


	/**
	 * Return the name of the missing component,
	 * if it was a lookup by name that failed.
	 */
	public String getComponentName() {
		return this.componentName;
	}

	/**
	 * Return the required type of component,
	 * if it was a lookup by type that failed.
	 */
	public Class getComponentType() {
		return this.componentType;
	}
}
