import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws SocketException, NoSuchAlgorithmException, UnknownHostException
    {
        if(args.length != 1)
        {
            System.err.println("Incorrect usage of program!\nUsage: java Main [port]");
            System.exit(-1);
        }

        Peer tmp = new Peer(Integer.parseInt(args[0]));
    }
}