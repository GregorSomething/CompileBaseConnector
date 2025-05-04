package me.gregorsomething.example;

import me.gregorsomething.database.Transactional;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.annotations.Statement;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository(value = """
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255),
            email VARCHAR(255)
        );
        """, additionalTypes = {EmailTypeReader.class})
public interface UserRepository extends Transactional<UserRepository> {

    @Query(value = "SELECT id, name, email FROM users WHERE id = [( id )];",
            onNoResultThrow = true)
    User byId(int id);

    @Query("SELECT id, name, email FROM users WHERE id = [( id )];")
    Optional<User> byIdOptional(int id);

    @Query("SELECT id, name, email FROM users;")
    List<User> getAll(int id);

    @Query(value = "SELECT count(id) FROM users;",
            defaultValue = "-1")
    int getUserCount();

    @Statement(value = "INSERT INTO users (name, email) " +
            "VALUES ([( name )], [( email.toString() )]);")
    void addUser(String name, Email email) throws SQLException;

    @Statement(value = "UPDATE users SET name = [( u.name )], email = [( u.email().toString() )] WHERE id = [( u.id )];")
    void updateUser(User u);
}
