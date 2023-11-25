import core.Input;
import core.Kademlia;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        Kademlia kademlia = null;
        if (args.length == 1)
        {
            kademlia = new Kademlia(Integer.parseInt(args[0]));
        }
        else if (args.length == 3)
        {
            kademlia = new Kademlia(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
        }
        else
        {
            System.err.println("Incorrect usage of program!\nUsage: java Main [port]\njava Main [port] [bootstrap_port] [bootstrap_ip]");
            System.exit(-1);
        }

        Input in = new Input(kademlia);
        in.ReceiveInput();
    }
}