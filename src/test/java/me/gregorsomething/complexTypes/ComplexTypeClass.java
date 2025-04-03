package me.gregorsomething.complexTypes;

import java.time.LocalDateTime;

public class ComplexTypeClass {
    private final int aaa;
    private final String bbb;
    private final LocalDateTime ccc;

    public ComplexTypeClass(int aaa, String bbb, LocalDateTime ccc) {
        this.aaa = aaa;
        this.bbb = bbb;
        this.ccc = ccc;
    }

    public static ComplexTypeClass of(int aaa, String bbb) {
        return new ComplexTypeClass(aaa, bbb, null);
    }
}
