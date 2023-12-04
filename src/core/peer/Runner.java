package core.peer;

import java.util.concurrent.Semaphore;

public abstract class Runner implements Runnable
{
    Runner()
    {
        SetState(true);
        this.m_togglelink = new Semaphore(1);
        this.m_toggled = false;
    }

    public final void SetState(boolean state) { this.m_running = state; }

    public void ToggleLink()
    {
        if(!m_toggled)
            this.m_togglelink.acquireUninterruptibly();
        else this.m_togglelink.release();

        m_toggled = !m_toggled;
    }

    protected void Toggle()
    {
        if(m_toggled)
        {
            m_togglelink.acquireUninterruptibly();
            m_togglelink.release();
        }
    }

    @Override
    public abstract void run();
    protected boolean m_toggled;
    protected Semaphore m_togglelink;

    protected boolean m_running;
}
