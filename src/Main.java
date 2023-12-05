import core.Input;
import core.Kademlia;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main
{
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException
    {
        Kademlia kademlia = new Kademlia();

        Input in = new Input(kademlia);
        in.ReceiveInput();
    }
}