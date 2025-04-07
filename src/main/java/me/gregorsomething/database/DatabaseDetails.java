package me.gregorsomething.database;

import lombok.Builder;

@Builder()
public record DatabaseDetails(String dbURL, String user, String password, String dbName, int maxPoolSize) { }
