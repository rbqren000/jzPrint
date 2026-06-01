package com.org.jzprinter.print.reprint;

import com.org.jzprinter.database.entity.PrintTaskEntity;

public interface ReprintStrategy {
    boolean reprint(PrintTaskEntity task, int pageIndex);
    boolean supportsCommandReprint();
}
