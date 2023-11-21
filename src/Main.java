import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException
    {
        if(args.length != 1)
        {
            System.err.println("Incorrect usage of program!\nUsage: java Main [port]");
            System.exit(-1);
        }

        Chord chord = new Chord(Integer.parseInt(args[0]));
        chord.m_peer.PrintFingerTable();
    }
}