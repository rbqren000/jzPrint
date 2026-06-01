package com.org.jzprinter.print;

import java.util.ArrayList;
import java.util.List;

public class PageSelector {

    /**
     * 根据打印模式从可用页码中筛选目标页
     *
     * @param availablePages 所有可用页码（如 [85,86,87,...,108]）
     * @param printMode      打印模式
     * @return 筛选后的目标页码列表
     */
    public static List<Integer> select(List<Integer> availablePages, PrintMode printMode) {
        if (availablePages == null || availablePages.isEmpty()) {
            return new ArrayList<>();
        }

        switch (printMode) {
            case ALL:
                return new ArrayList<>(availablePages);
            case ODD:
                return filterOdd(availablePages);
            case EVEN:
                return filterEven(availablePages);
            default:
                return new ArrayList<>(availablePages);
        }
    }

    private static List<Integer> filterOdd(List<Integer> pages) {
        List<Integer> result = new ArrayList<>();
        for (int page : pages) {
            if (page % 2 != 0) {
                result.add(page);
            }
        }
        return result;
    }

    private static List<Integer> filterEven(List<Integer> pages) {
        List<Integer> result = new ArrayList<>();
        for (int page : pages) {
            if (page % 2 == 0) {
                result.add(page);
            }
        }
        return result;
    }
}
