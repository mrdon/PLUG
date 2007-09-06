mySub = new Packages.com.atlassian.plugin.modulefactory.TestSuperclass("foobar");
mySub.someOtherMethod = function() {
    return "foobar";
};
itp = new JavaAdapter(TestSuperclass, mySub);
