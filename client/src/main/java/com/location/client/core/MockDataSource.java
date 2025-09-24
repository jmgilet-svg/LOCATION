package com.location.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockDataSource implements DataSourceProvider {

  private final List<Models.Agency> agencies = new ArrayList<>();
  private final List<Models.Client> clients = new ArrayList<>();

  public MockDataSource() {
    resetDemoData();
  }

  @Override
  public String getLabel() {
    return "MOCK";
  }

  @Override
  public void resetDemoData() {
    agencies.clear();
    clients.clear();
    var a1 = new Models.Agency(UUID.randomUUID().toString(), "Agence 1");
    var a2 = new Models.Agency(UUID.randomUUID().toString(), "Agence 2");
    agencies.add(a1);
    agencies.add(a2);
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Alpha", "facture@alpha.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Beta", "billing@beta.tld"));
    clients.add(new Models.Client(UUID.randomUUID().toString(), "Client Gamma", "compta@gamma.tld"));
    // Future: resources, interventions, documents (Diff 2/3)
  }

  @Override
  public List<Models.Agency> listAgencies() {
    return List.copyOf(agencies);
  }

  @Override
  public List<Models.Client> listClients() {
    return List.copyOf(clients);
  }

  @Override
  public void close() {}
}
