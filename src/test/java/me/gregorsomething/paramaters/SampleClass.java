package me.gregorsomething.paramaters;

import lombok.Getter;

public class SampleClass {

    public int aaa = 2;

    private int bbb = 3;
    @Getter
    private int ccc = 3;

    public int getBbb() {
        return bbb;
    }
}
