package com.smpcore;

/** Server ranks. Higher level includes everything below it. */
public enum Rank {
    MEMBER("&7", "Member", 0),
    MOD("&a", "Mod", 1),
    ADMIN("&c", "Admin", 2);

    private final String color;
    private final String display;
    private final int level;

    Rank(String color, String display, int level) {
        this.color = color;
        this.display = display;
        this.level = level;
    }

    public String color() { return color; }
    public String display() { return display; }
    public int level() { return level; }

    /** Chat / tab prefix, e.g. "&a[Mod] " */
    public String prefix() { return color + "[" + display + "] "; }

    public boolean atLeast(Rank other) { return level >= other.level; }

    public static Rank parse(String s) {
        if (s == null) return null;
        for (Rank r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.display.equalsIgnoreCase(s)) return r;
        }
        return null;
    }
}
