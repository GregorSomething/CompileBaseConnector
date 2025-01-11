package me.gregorsomething.paramaters;

import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;

@Repository("SELECT 1;")
public interface ParametersTestRepo {

    @Query(value = "SELECT [(sample.aaa)];", defaultValue = "-1")
    int testGetAaa(SampleClass sample);

    @Query(value = "SELECT [(sample.bbb)];", defaultValue = "-1")
    int testGetBbb(SampleClass sample);

    // Lombok values must use full get, because they are not known at the time
    @Query(value = "SELECT [(sample.getCcc())];", defaultValue = "-1")
    int testGetCcc(SampleClass sample);
}
