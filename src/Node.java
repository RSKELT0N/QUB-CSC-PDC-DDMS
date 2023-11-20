import java.net.DatagramSocket;
import java.net.SocketException;

public class Node
{
    public Node(String ip_address, int port) throws SocketException
    {
        m_socket = new DatagramSocket(port);
    }

    private DatagramSocket m_socket;
}
