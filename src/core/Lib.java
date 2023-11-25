package core;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Lib
{
    public static class Pair<F, S>
    {
        public Pair(F first, S second)
        {
            this.first = first;
            this.second = second;
        }

        public F first;
        public S second;
    }

    public static BigInteger SHA1(String input, BigInteger mod) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(input.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();

        StringBuilder hexString = new StringBuilder();

        for (byte b : digest) {
            hexString.append(String.format("%02x", b));
        }

        return new BigInteger(hexString.toString(), 16).mod(mod);
    }

    static public String FormatBytes(byte[] bytes, int length)
    {
        StringBuilder ret = new StringBuilder();
        for(int i = 0; i < length; i++)
        {
            ret.append((char)bytes[i]);
        }
        return ret.toString();
    }
}
