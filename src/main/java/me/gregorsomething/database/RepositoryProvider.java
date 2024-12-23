package me.gregorsomething.database;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class RepositoryProvider {
    private static final String IMPLEMENTATION_SUFFIX = "Imp";

    private RepositoryProvider() {}

    public static <T> T create(@NotNull Class<T> clazz, @NotNull Database database) {
        try {
            Constructor<?>[] constructors = clazz.getClassLoader().loadClass(clazz.getName() + IMPLEMENTATION_SUFFIX).getDeclaredConstructors();
            if (constructors.length != 1)
                throw new RepositoryCreationException("Class probably is not repository: " + clazz.getName());
            return (T) constructors[0].newInstance(database);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RepositoryCreationException("Repository implementation class file not found or was wrong!", e);
        } catch (InvocationTargetException e) {
            throw new RepositoryCreationException("Repository creation failed!", e.getCause());
        }
    }

    public static class RepositoryCreationException extends RuntimeException {
        public RepositoryCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public RepositoryCreationException(String message) {
            super(message);
        }
    }
}
