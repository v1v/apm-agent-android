package co.elastic.apm.android.sdk.services.network.type;

public final class CellNetworkType implements NetworkType {
    private final String subType;

    public CellNetworkType(String subType) {
        this.subType = subType;
    }

    @Override
    public String getName() {
        return "cell";
    }

    @Override
    public String getSubTypeName() {
        return subType;
    }
}