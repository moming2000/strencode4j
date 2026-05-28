package com.free.strencode;

/**
 * Hello world!
 *
 */
public class Demo
{
    public static void main( String[] args )
    {
        Crypto.initLoad("testapp1","WkGYlQOGDht7M5c8TThlbg==","http://localhost:8080");
        String enc = Crypto.encrypt("测试场景1", "hello world");
        System.out.println("enc: " + enc);

        String dec = Crypto.decrypt("测试场景1", enc);
        System.out.println("dec: " + dec);
    }
}
