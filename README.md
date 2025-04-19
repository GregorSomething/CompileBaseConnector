# Name TBD
## What is it?
The goal of this application is to generate code needed to 
communicate with database (in Java using JDBC). User, developer who 
uses it, only would need to import this application and make needed 
interface classes that this app could implement. It aims to keep things minimal, 
generating only things that user need.
## Why generate code not use reflection on runtime?
When most code is generated compiler can perform type checks normally, 
that it would not be abel to with reflection. It can help to avoid some wierd 
type issues that can come from unsafe reflection, as some times generics are 
not properly checked. Also, this can cut down a bit on runtime time, as analysis is 
done during compilation, and Just-In-Time (JIT) compiler can do more to optimize, 
compared to regular Java Reflection API.
## How to use this?
*Files for example:* [here](/src/test/java/me/gregorsomething/example)  
Lets say you have records:
```java filename=User.java
public record User(int id, String name, Email email) {}
```
```java filename=Email.java
public record Email(String name, String domain) {
    @Override
    public String toString() {
        return name + "@" + domain;
    }
}
```
And you have relational database with table
```sql
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255),
  email VARCHAR(255)
);
```
### Create Repository interface.
Now we can create interface annotated with `@Repository` annotatation like so
```java filename=UserRepository.java
@Repository(value = """
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255),
            email VARCHAR(255)
        );
        """, additionalTypes = {EmailTypeReader.class})
public interface UserRepository {}
```
Here value refers to statements that are run when repository was instantiated.  
And additionalTypes refers to classes for what to look for extra types. 
In this example email is stored in one column but in java object it is non-trivial object.
So user would have to define how to read this from one column. For that we can define following class:
```java filename=EmailTypeReader.java
public class EmailTypeReader {
    public static Email from(ResultSet rs, int column) throws SQLException {
        String emailString = rs.getString(column);
        if (emailString == null) return null;
        String[] parts = emailString.split("@");
        if (parts.length != 2) throw new IllegalStateException();
        return new Email(parts[0], parts[1]);
    }
}
```
Method name is not important, but it is important that it is public static, with wanted 
return type and takes ResultSet and according column index as its parameters.
### Creating query methods
These are placed into repository interface.
#### Selecting simple values, like int, Integer, long, boolean...
```java
@Query(value = "SELECT count(id) FROM users;", 
        defaultValue = "-1")
int getUserCount();
```
`@Query` annotation is used, where value is query.  
**Note!** For primitive types defaultValue is required.
#### Selecting to custom classes
```java
@Query(value = "SELECT id, name, email FROM users WHERE id = [( id )];",
        onNoResultThrow = true)
User byId(int id);
```
Here selected names must be the same as in java class, unsing `as ...` works as well.  
Parameters form methods are parsed into SQL, using their names, as seen with id. (more on that later)  
**Note!** `onNoResultThrow` throws no such element exception if result set contains no elements, 
it can be used with default value.  
##### List and optional example
```java
@Query("SELECT id, name, email FROM users WHERE id = [( id )];")
Optional<User> byIdOptional(int id);

@Query("SELECT id, name, email FROM users;")
List<User> getAll(int id);
```
#### Creating statement methods
```java
@Statement(value = "INSERT INTO users (name, email) " +
        "VALUES ([( name )], [( email.toString() )]);")
void addUser(String name, Email email) throws SQLException;

@Statement(value = "UPDATE users SET name = [( u.name )], email = [( u.email().toString() )] " + 
        " WHERE id = [( u.id )];")
void updateUser(User u);
```
To crete those we use `@Statement` annotation where value is SQL statement with apps parameters. 
Queries and Statements can throw SQLExceptions, but you can leave it out, 
then it will throw those as runtime exceptions.  
**Parameters** are taken from methods, if you have string name, then it gets placed to SQL query as `?` 
and set using prepared statement to avoid SQL injections. For other objects it types 
to resolve get method so if paramater named e has method getRes() method then that 
can be used as `e.res`, `e.getRes`, `e.getRes()`, if resolver fails to resolve 
something, it puts it as it is not code, and compiler can deal with it.

### TODO
- [ ] Implement paramater thingi in statements
- [ ] Make this file more readable.
- [ ] Document transactions
- [ ] Document how to get Repository implementation
