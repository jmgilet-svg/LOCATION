package com.location.client.core;

import java.util.List;

public interface DataSourceProvider extends AutoCloseable {
  String getLabel(); // "MOCK" or "REST"

  void resetDemoData(); // no-op for REST

  List<Models.Agency> listAgencies();

  List<Models.Client> listClients();
}
