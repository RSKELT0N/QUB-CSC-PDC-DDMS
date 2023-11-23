import core.CMDLine;
import core.Chord;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        Chord chord = null;
        if (args.length == 1)
        {
            chord = new Chord(Integer.parseInt(args[0]));
        }
        else if (args.length == 3)
        {
            chord = new Chord(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
        }
        else
        {
            System.err.println("Incorrect usage of program!\nUsage: java Main [port]\njava Main [port] [bootstrap_port] [bootstrap_ip]");
            System.exit(-1);
        }

        chord.CreatePeer();
        if(chord.GetPeer() != null)
            chord.GetPeer().Close();
    }
}