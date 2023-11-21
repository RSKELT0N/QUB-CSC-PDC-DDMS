import core.Chord;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        if (args.length != 1)
        {
            System.err.println("Incorrect usage of program!\nUsage: java Main [port]");
            System.exit(-1);
        }

        Chord chord = new Chord(Integer.parseInt(args[0]));
    }
}