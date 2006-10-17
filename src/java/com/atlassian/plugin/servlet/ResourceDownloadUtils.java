package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResourceDownloadUtils
{
    public static void serveFileImpl(HttpServletResponse httpServletResponse, InputStream in) throws IOException
    {
        OutputStream out = httpServletResponse.getOutputStream();

        try
        {
            byte[] buffer = new byte[16 * 1024];
            int read_count;

            while ((read_count = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read_count);
            }

            BaseFileServerServlet.log.info("Serving file done.");
        }
        catch (Exception e)
        {
            BaseFileServerServlet.log.info("I/O Error serving the requested file: " + e.toString());
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                } catch (IOException e)
                {
                    // noop.
                }
            }
            if (out != null)
            {
                try
                {
                    out.flush();

                    //out.close();
                }
                catch (Exception e)
                {
                    BaseFileServerServlet.log.info("Error flushing output stream: " + e.toString());
                }
            }
        }
    }
}
