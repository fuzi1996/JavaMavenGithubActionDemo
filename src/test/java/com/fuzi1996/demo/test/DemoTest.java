package com.fuzi1996.demo.test;

import org.junit.Assert;
import org.junit.Test;

public class DemoTest {
    @Test
    public void test(){
        // 这个就是为了报错,以让github action发布时跳过自动测试
        Assert.assertFalse(true);
    }
}
