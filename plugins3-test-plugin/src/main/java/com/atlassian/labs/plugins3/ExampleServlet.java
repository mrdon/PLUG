package com.atlassian.labs.plugins3;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ExampleServlet extends HttpServlet
{
    private final ScannedComponent scannedComponent;

    @Inject
    public ExampleServlet(ScannedComponent scannedComponent)
    {
        this.scannedComponent = scannedComponent;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        res.getWriter().write("Hello world.  Status: " + scannedComponent.getStatus());
        res.getWriter().close();
    }
}
