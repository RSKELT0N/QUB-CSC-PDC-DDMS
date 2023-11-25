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
                ExploreNewNeighbours();
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

    private void ExploreNewNeighbours() throws InterruptedException
    {
        Peer.RoutingTableEntry[] close_neighbours = m_peer.GetClosePeers(m_peer.m_id);
        for(var peer : close_neighbours)
        {
            this.m_peer.Send(peer.ip_address, peer.port, ("FIND_NODE_REQUEST" + " " + m_peer.m_id).getBytes());
        }
    }

    private void ShareAllDataItemsToNeighbours() throws InterruptedException
    {
        Peer.RoutingTableEntry[] close_peers = m_peer.GetClosePeers(m_peer.m_id);

        for(var data : m_peer.m_data_table.entrySet())
        {
            for(var peer: close_peers)
            {
                m_peer.Send(peer.ip_address, peer.port, ("STORE" + " " + data.getValue().first + "," + data.getValue().second).getBytes());
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
                this.m_peer.Send(peer.getValue().ip_address, peer.getValue().port, ("PING" + " " + m_peer.m_id).getBytes());
            }
        }
    }

    private void RemoveUnRespondedNeighbours()
    {
        for(var peer : m_peer.m_ping_vector.entrySet())
        {
            if(!(m_peer.GetPingStateForPeer(peer.getKey())))
            {
                m_peer.RemovePeerFromRoutingTable(peer.getKey());
            } else m_peer.SetPingStateForPeer(peer.getKey(), false);
        }
    }

    public void AddSendItem(Lib.Pair<String, Integer> p, byte[] s) throws InterruptedException
    {
        m_queue.put(new Lib.Pair<>(p, s));
    }

    private final int m_interval;
    private final Peer m_peer;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, byte[]>> m_queue;
}
