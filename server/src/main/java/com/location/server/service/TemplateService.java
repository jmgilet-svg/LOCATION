package com.location.server.service;

import com.location.server.domain.Intervention;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public String renderSubject(String template, Intervention intervention) {
    return render(template, intervention);
  }

  public String renderBody(String template, Intervention intervention) {
    return render(template, intervention);
  }

  private String render(String template, Intervention intervention) {
    if (template == null || template.isBlank()) {
      return "";
    }
    Map<String, String> values = new HashMap<>();
    values.put("agencyName", intervention.getAgency().getName());
    values.put("clientName", intervention.getClient().getName());
    values.put("interventionTitle", nullToEmpty(intervention.getTitle()));
    values.put("start", intervention.getStart().format(FORMATTER));
    values.put("end", intervention.getEnd().format(FORMATTER));
    String result = template;
    for (Map.Entry<String, String> entry : values.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
