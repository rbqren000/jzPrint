package com.org.jzprinter.network.model;

public class Edition {
    public String editionId;
    public String editionName;
    public String editionType;

    public boolean supportsStudent() {
        return editionType != null && editionType.contains("1");
    }

    public boolean supportsPrepareCode() {
        return editionType != null && editionType.contains("2");
    }
}
