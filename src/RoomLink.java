import java.util.UUID;

public class RoomLink {


    private final UUID id;
    private final ServerLink hostserver;
    private String name;    //name used for public rooms
    private String displayName; //name displayed to users
    private String localName;   //name given by user, null if unassigned

    public RoomLink(ServerLink host, UUID id) {
        this.hostserver = host;
        this.id = id;

        //TODO get name from server
    }

    public void setLocalName(String localName) {
        if (localName.isEmpty()) {
            //if an empty string is given, remove the local name
            localName = null;
            return;
        }
        this.localName = localName;
    }

    public void removeLocalName() {
        localName = null;
    }


}
