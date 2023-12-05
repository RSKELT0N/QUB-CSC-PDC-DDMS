package core.peer;

import core.Lib;

import java.util.concurrent.LinkedBlockingQueue;

class Heartbeat extends Runner
{
    Heartbeat(Peer peer, int interval)
    {
        super();
        this.m_peer = peer;
        this.m_queue = new LinkedBlockingQueue<>();
        this.m_interval = interval;
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            try
            {
                Toggle();
                ExploreCloseNeighbours();
                PingAllRoutingTable();
                ShareAllDataItemsToNeighbours();
                Thread.sleep((long) (m_interval * 1e3));
                RemoveUnRespondedNeighbours();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void ExploreCloseNeighbours() throws InterruptedException
    {
        m_peer.SendFindNode(m_peer.m_id);
    }

    private void ShareAllDataItemsToNeighbours() throws InterruptedException
    {

        for(var data : m_peer.m_data_table.entrySet())
        {
            Peer.RoutingTableEntry[] close_peers_to_key = m_peer.GetClosePeers(data.getKey(), m_peer.m_m_bits);
            for(var peer: close_peers_to_key)
            {
                m_peer.Send(peer.ip_address, peer.port, (m_peer.FormatCommand("STORE") + " " + data.getValue().first + "," + data.getValue().second).getBytes(),false);
            }
        }
    }

    private void PingAllRoutingTable() throws InterruptedException
    {
        for(var bucket : this.m_peer.m_routing_table.entrySet())
        {
            for(var peer : bucket.getValue().entrySet())
            {
                m_peer.SetPingStateForPeer(peer.getKey(), false);
                m_peer.SendPing(peer.getValue().ip_address, peer.getValue().port);
            }
        }
    }

    private void RemoveUnRespondedNeighbours()
    {
        try
        {
            for (var peer : m_peer.m_ping_vector.entrySet()) {
                if (!(m_peer.GetPingStateForPeer(peer.getKey()))) {
                    m_peer.RemovePeerFromRoutingTable(peer.getKey());
                } else m_peer.SetPingStateForPeer(peer.getKey(), false);
            }
        }
        catch (Exception e) {}
    }

    public void AddSendItem(Lib.Pair<String, Integer> p, byte[] s) throws InterruptedException
    {
        m_queue.put(new Lib.Pair<>(p, s));
    }

    private final int m_interval;
    private final Peer m_peer;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, byte[]>> m_queue;
}
