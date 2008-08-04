package it.com.atlassian.plugin.refimpl;

public class ParameterUtils {

    public static String getBaseUrl() {
        String port = System.getProperty("http.port");
        if (port == null) {
            port = "8080";
        }
        return "http://localhost:"+port+"/atlassian-plugins-refimpl";
    }
}
