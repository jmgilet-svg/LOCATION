package com.location.server.repo;

import com.location.server.domain.ClientContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientContactRepository extends JpaRepository<ClientContact, String> {
  List<ClientContact> findByClient_IdOrderByLastNameAscFirstNameAsc(String clientId);

  void deleteByClient_Id(String clientId);
}
