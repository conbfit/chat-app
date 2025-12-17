package src.ServerLinks;

public class ServerLinksHTTPS extends src.ServerLink{
    private final String address;
    public ServerLinksHTTPS(String address) {
        this.address = address;
    }
    @Override
    public String getAddress() {
        return address;
    }
    @Override
    public String getProtocol() {
        return "https";
    }
}