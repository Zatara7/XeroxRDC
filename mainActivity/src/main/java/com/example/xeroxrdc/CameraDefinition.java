package com.example.xeroxrdc;

/**
 * Camera Definition
 * This class represents a camera that the app can connect to.
 * The identifying property is the IP adddress of the mjpeg stream.
 * @author Daragh
 */

public class CameraDefinition
{
	private String address;	// IP address and port of the Mjpeg stream 
	private String name;	// Optional identifier for the user's convenience
//	private int connects; 	// Number of times connection was successful
	
	private final String defaultName = "New Camera";
	
	/**
	 * Constructor
	 * @param address IP address and port of the Mjpeg stream 
	 */
	public CameraDefinition(String address)
	{
		setAddress(address);
		setName(defaultName);
	}

	/**
	 * Constructor
	 * @param address IP address and port of the Mjpeg stream 
	 * @param name Optional identifier for the user's convenience
	 */
	public CameraDefinition(String address, String name)
	{
		setAddress(address);
		setName(name);
	}

	/**
	 * Example:
	 * CameraName - 192.168.0.1:8080
	 */
	public String toString()
	{
		return this.name + " - " + this.address;
	}
	
	public String getAddress()
	{
		return this.address;
	}
	
	public void setAddress(String address)
	{
		this.address = address;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
}
