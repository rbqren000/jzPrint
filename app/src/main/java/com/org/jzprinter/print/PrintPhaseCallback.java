package com.org.jzprinter.print;

public interface PrintPhaseCallback {

    enum Phase {
        PREPARE,
        TRANSFER,
        PRINT,
        STOPPED
    }

    void onPhaseChanged(Phase phase);

    void onPrepareStart(int totalPages);

    void onPreparePageProgress(int currentPage, int totalPages, int pageIndex);

    void onPrepareComplete();

    void onDataTransferStart(float totalSize);

    void onDataTransferProgress(int percentage);

    void onDataTransferComplete();

    void onPhysicalPrintStart(int totalPages);

    void onPhysicalPrintPageProgress(int printedPages, int totalPages, int pageIndex);

    void onPhysicalPrintComplete();

    void onPhaseError(String phase, String error);
}
