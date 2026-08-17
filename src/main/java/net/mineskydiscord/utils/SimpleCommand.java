package net.mineskydiscord.utils;

public record SimpleCommand(String name, String description) {

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}