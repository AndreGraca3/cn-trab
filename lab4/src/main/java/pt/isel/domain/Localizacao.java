package pt.isel.domain;

import com.google.cloud.firestore.GeoPoint;

public class Localizacao {
  public GeoPoint point;
  public Coordenadas coord;
  public String freguesia;
  public String local;

  public Localizacao() {}
}
