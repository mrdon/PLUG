<%@ page import="com.atlassian.plugin.Plugin" %>
<%@ page import="com.atlassian.plugin.refimpl.ContainerManager" %>
<%@ page import="org.osgi.framework.Bundle" %>
<html>

    <h2>Plugins</h2>
    <ul>
<% for (Object plugin : ContainerManager.getInstance().getPluginManager().getPlugins()) {
    Plugin p = (Plugin) plugin;
%>
        <li><%=p.getKey()%></li>
<% } %>
    </ul>

    <h2>Bundles</h2>
    <table>
        <tr>
            <th>Name</th>
            <th>Version</th>
            <th>State</th>
        </tr>

<% for (Bundle bundle : ContainerManager.getInstance().getOsgiContainerManager().getBundles()) {
%>
        <tr>
            <td>
                <%=bundle.getSymbolicName()%>
            </td>
            <td>
                <%=bundle.getHeaders().get("Bundle-Version")%>
            </td>
            <td>
                <%
                    String state = "N/A";
                    switch (bundle.getState()) {
                        case Bundle.INSTALLED : state = "Installed"; break;
                        case Bundle.ACTIVE : state = "Active"; break;
                        case Bundle.RESOLVED : state = "Resolved"; break;
                    }
                %>
                <%=state%>
            </td>    
        </tr>

<% } %>
    </table>
</html>