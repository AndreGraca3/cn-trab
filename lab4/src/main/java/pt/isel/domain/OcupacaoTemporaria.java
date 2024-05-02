package pt.isel.domain;

public class OcupacaoTemporaria {
    public int ID;
    public Localizacao location;
    public Evento event;

    public OcupacaoTemporaria() {
    }

    @Override
    public String toString() {
        return String.format("OcupacaoTemporaria[ID=%d, location={X=%s, Y=%s, freguesia=%s, local=%s, point=%s}]",
                ID,
                location.coord.X,
                location.coord.Y,
                location.freguesia,
                location.local,
                location.point
        );
    }
}
