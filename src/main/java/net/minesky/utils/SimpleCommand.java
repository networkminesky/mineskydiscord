package net.minesky.utils;

public class SimpleCommand {
    private final String name;
    private final String description;

    public SimpleCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
