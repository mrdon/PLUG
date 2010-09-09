import com.atlassian.labs.plugins3.ExampleServlet;
import com.atlassian.labs.plugins3.api.ApplicationInfo;
import com.atlassian.labs.plugins3.api.PluginDescriptor;
import com.atlassian.labs.plugins3.api.PluginDescriptorGenerator;

import java.net.URI;

import static com.atlassian.labs.plugins3.api.module.helper.LabelGenerator.label;
import static com.atlassian.labs.plugins3.api.module.helper.LinkGenerator.link;

/**
 *
 */
public class AtlassianPlugin implements PluginDescriptor
{
    public void config(ApplicationInfo context, PluginDescriptorGenerator plugin) throws Exception
    {
        plugin.info()
                .name("bob")
                .description("Some description");

        plugin.addWebItem("homeLink")
                .section("index.links")
                .weight(10)
                .label(label()
                    .value("Plugins 3 Servlet"))
                .link(link()
                    .id("homeLink")
                    .uri(new URI("/plugins/servlet/foo")));


        plugin.scanForModules(ExampleServlet.class.getPackage());

        /*
        plugin.addServletFilter("someOther")
                .name("filterMe")
                .addUrlPattern("/foo.*")
                .addDispatcher(FilterDispatcherCondition.REQUEST);

        if (context.getType() == ApplicationInfo.CONFLUENCE)
        {
            plugin.addWebSection("foo")
                    .location("system/confuence/actions")
                    .conditions(condition(Condition.class, true));
        }

        plugin.addWebSection("multiple")
                .location("system/actions")
                .conditions(
                        and(
                            or(
                                condition(Condition.class, false),
                                condition(Condition.class, true)
                            ),
                            and(condition(Condition.class, true))
                        ));

        plugin.addWebItem("someWebItem")
                .section("system/actions")
                .link(
                        link()
                                .uri(new URI("/foo.bar"))
                                .absolute(true)
                )
                .icon(
                        icon()
                            .height(500)
                            .width(100)
                            .link(link()
                                .uri(new URI("img.png")))
                );

        plugin.scanForModules(ExampleServlet.class.getPackage());

        //JiraPluginDescriptorGenerator jiraDescriptor = plugin.convertTo(JiraPluginDescriptorGenerator.class);
        

        plugin.add("someKey", new RestModuleDescriptor.Builder()
            .version("1.0")
            .path("/foo"));
        */
    }
}
