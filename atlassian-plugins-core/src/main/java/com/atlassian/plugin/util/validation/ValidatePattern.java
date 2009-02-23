package com.atlassian.plugin.util.validation;

import org.dom4j.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a pattern of rules against a dom4j node, patterned off of
 * <a href="http://www.schematron.com/">Schematron</a>
 *
 * @since 2.2.0
 */
public class ValidatePattern
{
    private final List<Rule> rules = new ArrayList<Rule>();

    private ValidatePattern() {
    }

    /**
     * @return a new pattern instance
     */
    public static ValidatePattern createPattern()
    {
        return new ValidatePattern();
    }

    /**
     * Adds a rule to the current pattern
     * @param context The xpath expression to determine one or more nodes to evaluate the rules against
     * @param tests A series of tests
     * @return this for chaining
     */
    public ValidatePattern rule(String context, RuleTest... tests)
    {
        rules.add(new Rule(context, tests));
        return this;
    }

    /**
     * Adds a rule to the current pattern, assuming the current context is "."
     * @param tests A series of tests
     * @return this for chaining
     */
    public ValidatePattern rule(RuleTest... tests)
    {
        rules.add(new Rule(".", tests));
        return this;
    }

    /**
     * Evaluates the rules against the provided node
     * @param element The node to evaluate
     * @throws ValidationException If a validation error occurs.  If wanting to resolve i18n keys
     * to messages, you can access the list of errors from the exception.
     */
    public void evaluate(Node element) throws ValidationException
    {
        List<String> errors = new ArrayList<String>();
        for (Rule rule : rules)
        {
            rule.evaluate(element, errors);
        }
        if (!errors.isEmpty())
        {
            if (errors.size() == 1)
            {
                throw new ValidationException(errors.get(0), errors);
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append("There were validation errors:\n");
                for (String msg : errors)
                {
                    sb.append("\t- ").append(msg).append("\n");
                }
                throw new ValidationException(sb.toString(), errors);
            }
        }
    }

    /**
     * Creates a test using the passed xpath expression
     * @param xpath The test expression
     * @return The rule to mutate
     */
    public static RuleTest test(String xpath)
    {
        return new RuleTest(xpath);
    }

    /**
     * A test within a rule
     */
    public static class RuleTest
    {
        private final String xpath;
        private String errorMessage;

        private RuleTest(String xpath)
        {
            this.xpath = xpath;
        }

        /**
         * The error message to use in the thrown exception if the test failes
         * @param msg The message
         * @return this for chaining
         */
        public RuleTest withError(String msg)
        {
            this.errorMessage = msg;
            return this;
        }

        private void evaluate(Node ctxNode, List<String> errors)
        {
            Object obj = ctxNode.selectObject(xpath);
            if (obj == null)
            {
                errors.add(errorMessage);
            }
            else if (obj instanceof Boolean && !((Boolean)obj).booleanValue())
            {
                errors.add(errorMessage);
            }
            else if (obj instanceof List && ((List<?>)obj).size() == 0)
            {
                errors.add(errorMessage);
            }

        }
    }

    /**
     * The rule as a series of tests
     */
    public static class Rule
    {
        private final String contextPattern;
        private final RuleTest[] tests;

        private Rule(String contextPattern, RuleTest[] tests)
        {
            this.contextPattern = contextPattern;
            this.tests = tests;
        }

        private void evaluate(Node e, List<String> errors)
        {
            @SuppressWarnings("unchecked")
            List<Node> contexts = e.selectNodes(contextPattern);

            if (contexts != null && contexts.size() > 0)
            {
                for (Node ctxNode : contexts)
                {
                    for (RuleTest test : tests)
                    {
                        test.evaluate(ctxNode, errors);
                    }
                }
            }
        }
    }

}
