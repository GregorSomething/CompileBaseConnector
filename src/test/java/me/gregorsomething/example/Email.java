package me.gregorsomething.example;

public record Email(String name, String domain) {
    @Override
    public String toString() {
        return name + "@" + domain;
    }
}
