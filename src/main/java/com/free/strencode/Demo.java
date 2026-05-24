package com.free.strencode;

/**
 * Hello world!
 *
 */
public class Demo
{
    public static void main( String[] args )
    {
        Crypto.initLoad("my_app","PwmkdDzs4GUY1Dkz39QMBg==","http://localhost:8080");
        String enc = Crypto.encrypt("测试", "hello world");
        System.out.println("enc: " + enc);

        String dec = Crypto.decrypt("测试", enc);
        System.out.println("dec: " + dec);
    }
}
