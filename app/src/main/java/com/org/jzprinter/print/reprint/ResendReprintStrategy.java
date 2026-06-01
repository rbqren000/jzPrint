package com.org.jzprinter.print.reprint;

import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.print.PrintEngine;

import java.util.List;

public class ResendReprintStrategy implements ReprintStrategy {
    private final PrintEngine printEngine;

    public ResendReprintStrategy(PrintEngine printEngine) {
        this.printEngine = printEngine;
    }

    @Override
    public boolean reprint(PrintTaskEntity task, int pageIndex) {
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        printed.remove(Integer.valueOf(pageIndex));
        task.setPrintedPages(IntegerListConverter.fromList(printed));
        task.setStatus(com.org.jzprinter.print.TaskStatus.IN_PROGRESS.getCode());
        task.setUpdatedAt(System.currentTimeMillis());
        printEngine.getDbExecutor().execute(() -> printEngine.getTaskRepo().update(task));
        printEngine.execute(task);
        return true;
    }

    @Override
    public boolean supportsCommandReprint() {
        return false;
    }
}
