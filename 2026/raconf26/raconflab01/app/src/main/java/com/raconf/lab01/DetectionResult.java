package com.raconf.lab01;

public class DetectionResult {

    public enum Layer { JAVA, NATIVE }
    public enum Category { PATH, PACKAGE, MAGISK, BUILD, EXEC, MOUNT, MAPS, EMU, UNKNOWN }

    public final String rawMessage;
    public final Layer layer;
    public final Category category;
    public final boolean isRooted;

    public DetectionResult(String rawMessage) {
        this.rawMessage = rawMessage;
        this.isRooted = true;
        this.layer = parseLayer(rawMessage);
        this.category = parseCategory(rawMessage);
    }

    private static Layer parseLayer(String msg) {
        if (msg.contains("[NATIVE]")) return Layer.NATIVE;
        return Layer.JAVA;
    }

    private static Category parseCategory(String msg) {
        if (msg.contains("[PATH]"))    return Category.PATH;
        if (msg.contains("[PKG]"))     return Category.PACKAGE;
        if (msg.contains("[MAGISK]"))  return Category.MAGISK;
        if (msg.contains("[BUILD]"))   return Category.BUILD;
        if (msg.contains("[EXEC]"))    return Category.EXEC;
        if (msg.contains("[MOUNT]"))   return Category.MOUNT;
        if (msg.contains("[MAPS]"))    return Category.MAPS;
        if (msg.contains("[EMU]"))     return Category.EMU;
        return Category.UNKNOWN;
    }

    public String getCategoryLabel() {
        switch (category) {
            case PATH:    return "Path";
            case PACKAGE: return "Package";
            case MAGISK:  return "Magisk";
            case BUILD:   return "Build Prop";
            case EXEC:    return "Exec";
            case MOUNT:   return "Mount";
            case MAPS:    return "Proc Maps";
            case EMU:     return "Emulator";
            default:      return "Unknown";
        }
    }

    public String getLayerLabel() {
        return layer == Layer.NATIVE ? "NATIVE" : "JAVA";
    }
}
