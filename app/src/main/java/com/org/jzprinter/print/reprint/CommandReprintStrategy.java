package com.org.jzprinter.print.reprint;

import com.org.jzprinter.database.entity.PrintTaskEntity;

public class CommandReprintStrategy implements ReprintStrategy {

    @Override
    public boolean reprint(PrintTaskEntity task, int pageIndex) {
        return false;
    }

    @Override
    public boolean supportsCommandReprint() {
        return false;
    }
}
