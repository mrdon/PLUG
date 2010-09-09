package com.atlassian.labs.plugins3.api.module.helper;

/**
 *
 */
public class LabelGenerator extends AbstractTextGenerator<LabelGenerator>
{

    public LabelGenerator()
    {
        super("label");
    }

    public static LabelGenerator label()
    {
        return new LabelGenerator();
    }

}
