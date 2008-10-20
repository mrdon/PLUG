package com.atlassian.plugin.resourcedownload.servlet;

public class DownloadException extends Exception
{
    public DownloadException()
    {
    }

    public DownloadException(String message)
    {
        super(message);
    }

    public DownloadException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DownloadException(Throwable cause)
    {
        super(cause);
    }
}