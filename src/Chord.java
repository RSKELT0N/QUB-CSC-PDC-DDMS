import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Chord
{
    Chord(int port) throws IOException, NoSuchAlgorithmException
    {
        DefinePeer(port);
        this.m_predecessor = this.m_peer;
        this.m_successor = this.m_peer;
    }

    private void DefinePeer(int port) throws SocketException, UnknownHostException, NoSuchAlgorithmException
    {
        this.m_peer = new Peer(port);
    }

    public Peer m_peer;
    public final Peer m_predecessor;
    public final Peer m_successor;
}
