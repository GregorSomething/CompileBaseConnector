package me.gregorsomething.complexTypes;

import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.processor.helpers.Pair;

import java.util.List;
import java.util.Optional;

@Repository("CREATE TABLE IF NOT EXISTS gs_test_database2 (aaa INT PRIMARY KEY, bbb TEXT);")
public interface ComplexTypeRepo {

    @Query(value = "SELECT bbb, aaa, 1 as ccc FROM gs_test_database2 LIMIT 1;")
    ComplexTypeClass getComplexType();

    @Query(value = "SELECT 2 as bbb, 'aaa' as aaa;")
    ComplexTypeClass getComplexTypeOfMethod();

    @Query(value = "SELECT bbb, aaa, 1 as ccc FROM gs_test_database2 LIMIT 1;")
    Optional<ComplexTypeClass> getComplexTypeOptional();

    @Query(value = "SELECT bbb, aaa, 1 as ccc FROM gs_test_database2;")
    List<ComplexTypeClass> getComplexTypeList();

    @Query(value = "SELECT 1 as 'left', 2 as 'right';")
    Optional<Pair<Integer, Long>> getPair();

    @Query(value = "SELECT 1 as 'left', 2 as 'right';")
    Pair<Integer, Long> getPair2();

}
