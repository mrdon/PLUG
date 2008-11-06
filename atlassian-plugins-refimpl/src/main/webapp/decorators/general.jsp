<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>

<html>
<head>
    <title><decorator:title default="Welcome!" /></title>
    <decorator:head />
    <link type="text/css" rel="StyleSheet" media="all" href="<%= request.getContextPath() %>/combined.css"/>
    <link type="text/css" rel="StyleSheet" media="all" href="<%= request.getContextPath() %>/global.css"/>
</head>

<body>
<div id="header">
<div id="header-top">
    <a id="logo" href="/secure/Dashboard.jspa">
        <img class="logo"
             src="<%= request.getContextPath() %>/jira_logo_small.png"
             width="111"
             height="30"
             border="0"
             alt="Atlassian JIRA">
    </a>

    <ul id="account-options">
        <li><a id="user_nav_bar_tmoore" href="/secure/ViewProfile.jspa?name=tmoore">Tim Moore [Atlassian]</a></li>
        <li>
            <a class="dropdownmenu"
               id="saved_filters"
               title="View and open your favourite filters"
               href="/secure/FavouriteFilters.jspa"
               onClick="return false;"><span>Filters</span></a>
        </li>
        <li>
            <a href="/logout" title="Log out and cancel any automatic login.">Log Out</a>
        </li>
        <li class="icon">
            <a rel="nofollow"
               href="http://jira.atlassian.com/secure/Dashboard.jspa?decorator=printable"><img src="<%= request.getContextPath() %>/print.gif"
                                                                                               width="16"
                                                                                               height="16"
                                                                                               align="absmiddle"
                                                                                               border="0"
                                                                                               alt="View a printable version of the current page."
                                                                                               title="View a printable version of the current page."></a>
        </li>
        <li class="icon">

            <a class="helpLink"
               href="http://www.atlassian.com/software/jira/docs/v3.13/index.html?clicked=jirahelp"
               target="_jirahelp">

                <img src="<%= request.getContextPath() %>/help_blue.gif" width="16" height="16" align="absmiddle"
                     title="Get online help about Using and setting up JIRA"
                    /></a>

        </li>
    </ul>


</div>
<div id="menu">

    <ul id="main-nav">
        <li>
            <a href="/secure/Dashboard.jspa"
               class="selected" id="home_link" title="A configurable overview of JIRA" accessKey="h"><u>h</u>ome</a>
        </li>
        <li>
            <a href="/secure/BrowseProject.jspa"
               id="browse_link" title="Browse the projects you have permissions to view" accessKey="b"><u>b</u>rowse
                projects</a>
        </li>
        <li>
            <a href="/secure/IssueNavigator.jspa"
               id="find_link"
               title="Find issues in the projects you have permissions to view"
               accessKey="f"><u>f</u>ind issues</a>
        </li>
        <li>
            <a href="/secure/CreateIssue!default.jspa"
               id="create_link" title="Create a new issue / bug / feature request / etc" accessKey="c"><u>c</u>reate
                new issue</a>
        </li>
        <li>
            <a href="/secure/project/ViewProjects.jspa"
               id="admin_link" title="Manage this JIRA instance" accessKey="a"><u>a</u>dministration</a>
        </li>
        <li>
            <a href="/secure/PlanningBoard.jspa?decorator=none"
               id="planningboardmenu" title="Go to the Planning board of the current Project" accessKey="p"><u>p</u>lanning
                board</a>
        </li>
        <li>
            <a href="/secure/TaskBoard.jspa?decorator=none"
               id="taskboardmenu" title="Go to the Task board of the current Project" accessKey="t"><u>t</u>ask
                board</a>
        </li>
    </ul>


    <form id="quicksearch" action="/secure/QuickSearch.jspa" method="post">
        <label onclick="quickSearchTextBoxSetFocus();" onmouseover="showToolTip()" onmouseout="hideToolTip()">&nbsp;
            &nbsp;<u>Q</u>uick Search:</label>
        <input id="quickSearchInput"
               class="quickSearchInput"
               title="Go directly to an issue by typing a valid issue key, or run a free-text search."
               type="text"
               size="25"
               name="searchString"
               accessKey="q"
               valign="absmiddle" />
    </form>
    <div onmouseover="recordInTip()" onmouseout="recordOutTip()" id="quicksearchhelp" class="informationBox"
         style="display: none; text-align: center; width: 20em; position: absolute; top: 55px; right: 10px; padding: 0.5em;">
        Learn more about
        <a href='http://www.atlassian.com/software/jira/docs/v3.13/quicksearch.html?clicked=jirahelp'
           target='_jirahelp'>Quick Search</a>
    </div>
</div>
</div>
<decorator:body />
<div class="footer" style="clear:both;">
    <table border="0" cellpadding="0" cellspacing="0" width="100%">
        <tr>
            <td height="12" style="background-image:url(<%= request.getContextPath() %>/border_bottom.gif)"></td>
        </tr>
    </table>

<span class="poweredbymessage">
Powered by <a href="http://www.atlassian.com/software/jira" class="smalltext">Atlassian JIRA</a>
the Professional <a href="http://www.atlassian.com/software/jira">Issue Tracker</a>.
<span style="color: #666666;">(Enterprise Edition, Version: 3.13.1-#333)</span>    - <a href="http://jira.atlassian.com/default.jsp?clicked=footer">Bug/feature
    request</a>
- <a href="http://www.atlassian.com/about/connected.jsp?s_kwcid=jira-stayintouch">Atlassian news</a>
- <a href="/secure/Administrators.jspa">Contact Administrators</a>
</span>


</div>
</body>
</html>