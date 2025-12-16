import java.util.regex.Pattern;

public abstract class ServerLink_HTTPS extends ServerLink {
    //Class representing a reference to a server that will be reached over HTTPS
    //  specified using either domain names or ip addresses
    //  constructor validates domain/IP and throws IllegalArgumentException if something bad is passed
    //      NOTE: There is potential for users to mistakenly input a TOR hidden service "blahblahblah.onion" as an HTTPS
    //          address. Because of custom TLDs it is unadvisable to invalidate ".onion" as a TLD. Instead, a warning in
    //          frontend should be shown to users, something like "to connect to a TOR hidden service, use protocol TOR"

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*" +
                    "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
                    "(?:\\.[a-zA-Z]{2,})?$"
    );
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|" +           // 1:2:3:4:5:6:7:8
                    "([0-9a-fA-F]{1,4}:){1,7}:|" +                          // 1::
                    "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +          // 1::8
                    "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +  // 1::7:8
                    "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +  // 1::6:7:8
                    "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +  // 1::5:6:7:8
                    "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +  // 1::4:5:6:7:8
                    "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +        // 1::3:4:5:6:7:8
                    ":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +                      // ::2:3:4:5:6:7:8, ::
                    "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|" +      // fe80::...%eth0 (link-local)
                    "::(ffff(:0{1,4}){0,1}:){0,1}" +
                    "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}" +
                    "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|" +           // ::ffff:192.0.2.1
                    "([0-9a-fA-F]{1,4}:){1,4}:" +
                    "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}" +
                    "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$"
    );
    public static final String PROTOCOL = "HTTPS";

    private final String address;
    private final boolean isDomain;

    public ServerLink_HTTPS(String addr) throws IllegalArgumentException {
        boolean test = true;

        //check for and reject empty address
        if (addr == null) {
            test = false;
        }
        //check for and reject any address including a path
        if (addr.contains("/")) {
            test = false;
        }

        if (DOMAIN_PATTERN.matcher(addr).matches() && !addr.contains(":") && test) {
            this.address = addr;
            this.isDomain = true;
            return;
        }

        if (IPV4_PATTERN.matcher(addr).matches() || IPV6_PATTERN.matcher(addr).matches() && test) {
            this.address = addr;
            this.isDomain = false;
        }

        throw new IllegalArgumentException(
                "Cannot construct new ServerLink: '"+addr+"' is not a valid domain name, IPV4, or IPV6 address"
        );
    }

    public String getProtocol() {return PROTOCOL;}
    public String getAddress() {return address;}
    public String toString() {return PROTOCOL+"://"+address;}

}
