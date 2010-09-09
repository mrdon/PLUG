package com.atlassian.labs.plugins3.api.module.helper;

/**
 *
 */
public class TooltipGenerator extends AbstractTextGenerator<TooltipGenerator>
{

    public TooltipGenerator()
    {
        super("tooltip");
    }

    public static TooltipGenerator tooltip()
    {
        return new TooltipGenerator();
    }

}
