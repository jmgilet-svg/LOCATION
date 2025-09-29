package com.location.server.service;

public interface MailGateway {
  record Mail(String to, String subject, String body, byte[] pdfAttachment, String filename) {}

  void send(Mail mail);

  class DevMailGateway implements MailGateway {
    @Override
    public void send(Mail mail) {
      String attachment = mail.filename() == null ? "(aucune pièce jointe)" : mail.filename();
      System.out.println(
          "[DEV MAIL] to=" + mail.to() + " subject=" + mail.subject() + " attachment=" + attachment);
    }
  }
}
