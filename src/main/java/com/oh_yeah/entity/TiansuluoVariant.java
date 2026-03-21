package com.oh_yeah.entity;

import org.jetbrains.annotations.Nullable;

public enum TiansuluoVariant {
    SCARF_PINK("scarf_pink"),
    BASE("base"),
    ;

    private final String id;

    TiansuluoVariant(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static @Nullable TiansuluoVariant byId(String id) {
        if ("classic".equals(id)) {
            return SCARF_PINK;
        }
        for (TiansuluoVariant variant : values()) {
            if (variant.id.equals(id)) {
                return variant;
            }
        }
        return null;
    }
}
