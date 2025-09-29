package com.location.server.api.v1;

import com.location.server.api.v1.dto.ApiV1Dtos.ClientDto;
import com.location.server.api.v1.dto.ApiV1Dtos.ContactDto;
import com.location.server.api.v1.dto.ApiV1Dtos.SaveClientRequest;
import com.location.server.api.v1.dto.ApiV1Dtos.SaveContactRequest;
import com.location.server.domain.Client;
import com.location.server.domain.ClientContact;
import com.location.server.repo.ClientContactRepository;
import com.location.server.repo.ClientRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientAdminController {
  private final ClientRepository clientRepository;
  private final ClientContactRepository contactRepository;

  public ClientAdminController(
      ClientRepository clientRepository, ClientContactRepository contactRepository) {
    this.clientRepository = clientRepository;
    this.contactRepository = contactRepository;
  }

  @GetMapping
  public List<ClientDto> listClients() {
    return clientRepository.findAll().stream().map(ClientDto::of).collect(Collectors.toList());
  }

  @PostMapping
  @Transactional
  public ClientDto saveClient(@Valid @RequestBody SaveClientRequest request) {
    Client client;
    if (request.id() != null && !request.id().isBlank()) {
      client =
          clientRepository
              .findById(request.id())
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client"));
    } else {
      client = new Client(UUID.randomUUID().toString(), request.name(), request.email());
    }
    client.setName(request.name().trim());
    client.setEmail(normalize(request.email()));
    client.setPhone(normalize(request.phone()));
    client.setAddress(normalize(request.address()));
    client.setZip(normalize(request.zip()));
    client.setCity(normalize(request.city()));
    client.setVatNumber(normalize(request.vatNumber()));
    client.setIban(normalize(request.iban()));
    Client saved = clientRepository.save(client);
    return ClientDto.of(saved);
  }

  @DeleteMapping("/{id}")
  @Transactional
  public ResponseEntity<Void> deleteClient(@PathVariable String id) {
    if (!clientRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client");
    }
    contactRepository.deleteByClient_Id(id);
    clientRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{clientId}/contacts")
  public List<ContactDto> listContacts(@PathVariable String clientId) {
    if (!clientRepository.existsById(clientId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client");
    }
    return contactRepository.findByClient_IdOrderByLastNameAscFirstNameAsc(clientId).stream()
        .map(ContactDto::of)
        .collect(Collectors.toList());
  }

  @PostMapping("/{clientId}/contacts")
  @Transactional
  public ContactDto saveContact(
      @PathVariable String clientId, @Valid @RequestBody SaveContactRequest request) {
    if (request.clientId() != null && !request.clientId().isBlank()) {
      if (!request.clientId().equals(clientId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client mismatch");
      }
    }
    Client client =
        clientRepository
            .findById(clientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client"));
    ClientContact contact;
    if (request.id() != null && !request.id().isBlank()) {
      contact =
          contactRepository
              .findById(request.id())
              .orElseGet(() -> new ClientContact(request.id(), client));
      contact.setClient(client);
    } else {
      contact = new ClientContact(UUID.randomUUID().toString(), client);
    }
    contact.setFirstName(normalize(request.firstName()));
    contact.setLastName(normalize(request.lastName()));
    contact.setEmail(normalize(request.email()));
    contact.setPhone(normalize(request.phone()));
    ClientContact saved = contactRepository.save(contact);
    return ContactDto.of(saved);
  }

  @DeleteMapping("/contacts/{id}")
  @Transactional
  public ResponseEntity<Void> deleteContact(@PathVariable String id) {
    if (!contactRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact");
    }
    contactRepository.deleteById(id);
    return ResponseEntity.noContent().build();
}

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
