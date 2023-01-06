package com.buuz135.functionalstorage.util;

import io.github.fabricators_of_create.porting_lib.util.FluidTextUtil;

import java.text.DecimalFormat;

public class NumberUtils {

    private static DecimalFormat formatterWithUnits = new DecimalFormat("####0.#");

    public static String getFormatedBigNumber(long number) {
        if (number >= 1000000000) { //BILLION
            float numb = number / 1000000000F;
            return formatterWithUnits.format(numb) + "B";
        } else if (number >= 1000000) { //MILLION
            float numb = number / 1000000F;
            if (number > 100000000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "M";
        } else if (number >= 1000) { //THOUSANDS
            float numb = number / 1000F;
            if (number > 100000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "K";
        }
        return String.valueOf(number);
    }

    public static String getFormatedFluidBigNumber(long number) {
        number = number / 81;
        if (number < 1000) return String.valueOf(number) + " mB";
        if (number >= 1000000000) { //BILLION
            float numb = number / 1000000000F;
            return formatterWithUnits.format(numb) + "B B";
        } else if (number >= 1000000) { //MILLION
            float numb = number / 1000000F;
            if (number > 100000000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + "M B";
        } else if (number >= 1000) { //THOUSANDS
            float numb = number / 1000F;
            if (number > 100000) numb = Math.round(numb);
            return formatterWithUnits.format(numb) + " B";
        }
        return String.valueOf(number) + " B";
    }
}

