import java.lang.ref.PhantomReference;

public abstract class ServerLink {
    //Link to a server over HTTPS. Serverlinks using other protocols will extend this class.

    public abstract String getAddress();
    public abstract String getProtocol();
}
