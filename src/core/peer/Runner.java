package core.peer;

public abstract class Runner implements Runnable
{
    Runner() { SetState(true); }
    public final void SetState(boolean state) { this.m_running = state; }

    @Override
    public abstract void run();

    protected boolean m_running;
}
