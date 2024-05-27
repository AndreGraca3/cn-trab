package pt.isel.domain;

public class InstancesList {

    private final String[] instanceIps;

    public InstancesList(String[] instances) {
        this.instanceIps = instances;
    }

    public String[] getInstanceIps() {
        return instanceIps;
    }
}
