package com.location.client.ui;

import com.location.client.core.DataSourceProvider;
import com.location.client.core.Models;
import com.location.client.ui.accordion.CollapsibleSection;
import com.location.client.ui.icons.SvgIconLoader;
import com.location.client.ui.uikit.TableUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import com.location.client.ui.uikit.Toasts;

public class ResourceExplorerFrame extends JFrame {
  private final DataSourceProvider dataSourceProvider;
  private final JComboBox<String> cbFilter = new JComboBox<>();
  private final JPanel accordion = new JPanel();
  private final JPanel detailPanel = new JPanel(new BorderLayout());
  private final static Models.ResourceType fallbackType = new Models.ResourceType(null, "Sans type", "hook.svg");
  private List<Models.ResourceType> availableTypes = new ArrayList<>();
  private boolean typeSupport = true;

  public ResourceExplorerFrame(DataSourceProvider dsp) {
    super("Ressources — Explorer & Configurer");
    this.dataSourceProvider = dsp;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout(8, 8));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
    header.add(new JLabel("Filtrer par type:"));
    cbFilter.addActionListener(e -> buildAccordion());
    header.add(cbFilter);
    JButton btNew = new JButton(new AbstractAction("Nouvelle ressource") {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        createResource();
      }
    });
    header.add(btNew);
    add(header, BorderLayout.NORTH);

    accordion.setLayout(new javax.swing.BoxLayout(accordion, javax.swing.BoxLayout.Y_AXIS));
    JScrollPane scroll = new JScrollPane(accordion);
    detailPanel.add(new JLabel("Sélectionnez une ressource pour afficher les détails."), BorderLayout.CENTER);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, detailPanel);
    split.setResizeWeight(0.6);
    add(split, BorderLayout.CENTER);

    setSize(1100, 650);
    setLocationRelativeTo(null);

    reloadTypes();
    refreshFilterItems(null);
    buildAccordion();
  }

  private void reloadTypes() {
    if (!typeSupport) {
      availableTypes = List.of();
      return;
    }
    try {
      availableTypes = new ArrayList<>(dataSourceProvider.listResourceTypes());
      availableTypes.sort(Comparator.comparing(Models.ResourceType::name, String.CASE_INSENSITIVE_ORDER));
    } catch (UnsupportedOperationException ex) {
      typeSupport = false;
      availableTypes = List.of();
      cbFilter.setEnabled(false);
    }
  }

  private void refreshFilterItems(String preferredSelection) {
    cbFilter.removeAllItems();
    cbFilter.addItem("TOUS");
    if (typeSupport && !availableTypes.isEmpty()) {
      for (Models.ResourceType type : availableTypes) {
        cbFilter.addItem(type.name());
      }
    }
    cbFilter.addItem(fallbackType.name());
    if (preferredSelection != null) {
      for (int i = 0; i < cbFilter.getItemCount(); i++) {
        if (preferredSelection.equals(cbFilter.getItemAt(i))) {
          cbFilter.setSelectedIndex(i);
          return;
        }
      }
    }
    cbFilter.setSelectedIndex(0);
  }

  private void buildAccordion() {
    String previousFilter = cbFilter.getSelectedItem() == null ? null : cbFilter.getSelectedItem().toString();
    reloadTypes();
    refreshFilterItems(previousFilter);
    accordion.removeAll();
    Map<String, Models.ResourceType> typeById =
        availableTypes.stream().collect(Collectors.toMap(Models.ResourceType::id, t -> t));
    Map<String, List<Models.Resource>> grouped = new HashMap<>();
    String filter = cbFilter.getSelectedItem() == null ? "TOUS" : cbFilter.getSelectedItem().toString();
    for (Models.Resource resource : dataSourceProvider.listResources()) {
      Models.ResourceType type = null;
      if (typeSupport) {
        String typeId = getTypeId(resource.id());
        if (typeId != null) {
          type = typeById.get(typeId);
        }
      }
      String typeName = type == null ? fallbackType.name() : type.name();
      if (!"TOUS".equals(filter) && !typeName.equals(filter)) {
        continue;
      }
      grouped.computeIfAbsent(typeName, k -> new ArrayList<>()).add(resource);
    }

    List<String> orderedKeys = new ArrayList<>(grouped.keySet());
    orderedKeys.sort(String.CASE_INSENSITIVE_ORDER);
    for (String typeName : orderedKeys) {
      List<Models.Resource> resources = grouped.get(typeName);
      CollapsibleSection section = new CollapsibleSection(typeName + " (" + resources.size() + ")");
      section.setExpanded(true);
      JPanel list = new JPanel();
      list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
      for (Models.Resource resource : resources) {
        JButton row = new JButton(resource.name() + "  —  " + nullToEmpty(resource.licensePlate()));
        row.setHorizontalAlignment(SwingConstants.LEFT);
        row.setBackground(Color.WHITE);
        row.setOpaque(true);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.addActionListener(e -> showDetails(resource));
        Models.ResourceType type = findTypeByName(typeName);
        ImageIcon icon = SvgIconLoader.load(type == null ? fallbackType.iconName() : type.iconName(), 18);
        row.setIcon(icon);
        list.add(row);
      }
      section.setContent(list);
      accordion.add(section);
    }
    accordion.add(javax.swing.Box.createVerticalGlue());
    accordion.revalidate();
    accordion.repaint();
    detailPanel.revalidate();
    detailPanel.repaint();
  }

  private Models.ResourceType findTypeByName(String name) {
    if (name == null) {
      return null;
    }
    for (Models.ResourceType type : availableTypes) {
      if (type.name().equals(name)) {
        return type;
      }
    }
    if (fallbackType.name().equals(name)) {
      return fallbackType;
    }
    return null;
  }

  private String getTypeId(String resourceId) {
    if (!typeSupport) {
      return null;
    }
    try {
      return dataSourceProvider.getResourceTypeForResource(resourceId);
    } catch (UnsupportedOperationException ex) {
      typeSupport = false;
      cbFilter.setEnabled(false);
      return null;
    }
  }

  private void createResource() {
    Models.ResourceType chosenType = null;
    if (typeSupport && !availableTypes.isEmpty()) {
      DefaultComboBoxModel<Models.ResourceType> model =
          new DefaultComboBoxModel<>(availableTypes.toArray(new Models.ResourceType[0]));
      JComboBox<Models.ResourceType> combo = new JComboBox<>(model);
      combo.setRenderer(typeRenderer());
      int option =
          JOptionPane.showConfirmDialog(
              this,
              combo,
              "Type de ressource",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);
      if (option == JOptionPane.OK_OPTION) {
        chosenType = (Models.ResourceType) combo.getSelectedItem();
      } else {
        return;
      }
    }
    Models.Resource saved =
        dataSourceProvider.saveResource(
            new Models.Resource(
                null,
                "Nouvelle ressource",
                "",
                0x5096FF,
                dataSourceProvider.getCurrentAgencyId(),
                "",
                null));
    if (typeSupport && chosenType != null) {
      try {
        dataSourceProvider.setResourceTypeForResource(saved.id(), chosenType.id());
      } catch (UnsupportedOperationException ex) {
        Toasts.error(
            SwingUtilities.getWindowAncestor(this),
            "Attribution de type indisponible: " + ex.getMessage());
        typeSupport = false;
      }
    }
    buildAccordion();
    showDetails(saved);
  }

  private ListCellRenderer<? super Models.ResourceType> typeRenderer() {
    return new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
          javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel base =
            (JLabel)
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Models.ResourceType type) {
          base.setText(type.name());
        } else {
          base.setText(fallbackType.name());
        }
        return base;
      }
    };
  }

  private void showDetails(Models.Resource resource) {
    detailPanel.removeAll();
    detailPanel.add(
        new ResourceSidePanel(
            dataSourceProvider, resource, availableTypes, typeSupport, this::onResourceUpdated),
        BorderLayout.CENTER);
    detailPanel.revalidate();
    detailPanel.repaint();
  }

  private void onResourceUpdated(Models.Resource updated) {
    buildAccordion();
    Toasts.success(SwingUtilities.getWindowAncestor(this), "Ressource mise à jour");
    showDetails(updated);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static class ResourceSidePanel extends JPanel {
    private final DataSourceProvider dataSourceProvider;
    private Models.Resource resource;
    private final List<Models.ResourceType> types;
    private final boolean typesSupported;
    private final Consumer<Models.Resource> callback;
    private final JTextField tfName = new JTextField(18);
    private final JTextField tfPlate = new JTextField(10);
    private final JTextField tfTags = new JTextField(18);
    private final JSpinner spCapacity = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JTextField tfRate = new JTextField(8);
    private final JButton btColor = new JButton("Couleur…");
    private final JComboBox<Models.ResourceType> cbType = new JComboBox<>();
    private final JLabel lbTypeIcon = new JLabel();
    private Integer colorRgb;
    private boolean rateSupported = true;
    private final DefaultTableModel unavailabilityModel =
        new DefaultTableModel(new Object[] {"Début", "Fin", "Motif"}, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false;
          }
        };
    private final JTable unavailabilityTable = new JTable(unavailabilityModel);
    private final JTextField tfUnavailabilitySearch = new JTextField(18);
    private List<Models.Unavailability> unavailabilityCache = List.of();
    private final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH).withZone(ZoneId.systemDefault());

    ResourceSidePanel(
        DataSourceProvider dsp,
        Models.Resource resource,
        List<Models.ResourceType> types,
        boolean typesSupported,
        Consumer<Models.Resource> callback) {
      super(new BorderLayout(8, 8));
      this.dataSourceProvider = dsp;
      this.resource = resource;
      this.types = new ArrayList<>(types);
      this.typesSupported = typesSupported;
      this.callback = callback;

      JPanel form = new JPanel(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.insets = new Insets(6, 6, 6, 6);
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1.0;
      int y = 0;

      addRow(form, c, y++, "Nom", tfName);
      addRow(form, c, y++, "Plaque", tfPlate);
      addRow(form, c, y++, "Tags", tfTags);
      addRow(form, c, y++, "Capacité (t)", spCapacity);
      addRow(form, c, y++, "Prix/jour (€)", tfRate);

      JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      colorPanel.add(btColor);
      addRow(form, c, y++, "Couleur", colorPanel);

      JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
      typePanel.add(cbType);
      typePanel.add(lbTypeIcon);
      addRow(form, c, y++, "Type", typePanel);

      add(form, BorderLayout.NORTH);

      btColor.setFocusPainted(false);
      btColor.addActionListener(e -> chooseColor());
      lbTypeIcon.setPreferredSize(new java.awt.Dimension(36, 36));

      configureTypeCombo();

      JPanel bottom = new JPanel(new BorderLayout());
      bottom.setBorder(javax.swing.BorderFactory.createTitledBorder("Indisponibilités"));
      unavailabilityTable.setRowHeight(24);
      bottom.add(new JScrollPane(unavailabilityTable), BorderLayout.CENTER);
      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);
      toolbar.add(new JButton(new AbstractAction("Ajouter") {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          addUnavailability();
        }
      }));
      toolbar.add(new JButton(new AbstractAction("Supprimer") {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          deleteUnavailability();
        }
      }));
      JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
      searchPanel.add(new JLabel("Recherche"));
      tfUnavailabilitySearch.setColumns(16);
      searchPanel.add(tfUnavailabilitySearch);
      TableUtils.applySearch(unavailabilityTable, tfUnavailabilitySearch, 250);
      JPanel header = new JPanel(new BorderLayout(8, 0));
      header.add(toolbar, BorderLayout.WEST);
      header.add(searchPanel, BorderLayout.CENTER);
      bottom.add(header, BorderLayout.NORTH);
      add(bottom, BorderLayout.CENTER);

      JButton btSave = new JButton(new AbstractAction("Enregistrer") {
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
          save();
        }
      });
      JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      south.add(btSave);
      add(south, BorderLayout.SOUTH);

      load();
    }

    private void configureTypeCombo() {
      cbType.setRenderer(
          new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
              JLabel base =
                  (JLabel)
                      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
              if (value instanceof Models.ResourceType type) {
                base.setText(type.name());
              } else {
                base.setText("(aucun)");
              }
              return base;
            }
          });
      cbType.removeAllItems();
      cbType.addItem(null);
      if (typesSupported) {
        for (Models.ResourceType type : types) {
          cbType.addItem(type);
        }
      } else {
        cbType.setEnabled(false);
      }
      cbType.addActionListener(e -> updateTypeIcon());
      updateTypeIcon();
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, Component field) {
      c.gridx = 0;
      c.gridy = row;
      c.weightx = 0;
      c.fill = GridBagConstraints.NONE;
      panel.add(new JLabel(label), c);
      c.gridx = 1;
      c.weightx = 1.0;
      c.fill = GridBagConstraints.HORIZONTAL;
      panel.add(field, c);
    }

    private void load() {
      tfName.setText(resource.name());
      tfPlate.setText(resource.licensePlate());
      tfTags.setText(resource.tags());
      spCapacity.setValue(resource.capacityTons() == null ? 0 : resource.capacityTons());
      colorRgb = resource.colorRgb();
      updateColorButton();
      loadRate();
      selectCurrentType();
      loadUnavailabilities();
    }

    private void loadRate() {
      try {
        double rate = dataSourceProvider.getResourceDailyRate(resource.id());
        tfRate.setText(String.format(Locale.FRENCH, "%.2f", rate));
      } catch (UnsupportedOperationException ex) {
        tfRate.setText("N/A");
        tfRate.setEnabled(false);
        rateSupported = false;
      }
    }

    private void selectCurrentType() {
      if (!typesSupported) {
        return;
      }
      try {
        String typeId = dataSourceProvider.getResourceTypeForResource(resource.id());
        if (typeId == null) {
          cbType.setSelectedItem(null);
          return;
        }
        for (int i = 0; i < cbType.getItemCount(); i++) {
          Models.ResourceType item = cbType.getItemAt(i);
          if (item != null && typeId.equals(item.id())) {
            cbType.setSelectedItem(item);
            return;
          }
        }
        cbType.setSelectedItem(null);
      } catch (UnsupportedOperationException ex) {
        cbType.setEnabled(false);
      }
    }

    private void loadUnavailabilities() {
      try {
        unavailabilityCache = dataSourceProvider.listUnavailability(resource.id());
      } catch (UnsupportedOperationException ex) {
        unavailabilityCache = List.of();
      }
      unavailabilityModel.setRowCount(0);
      for (Models.Unavailability u : unavailabilityCache) {
        unavailabilityModel.addRow(
            new Object[] {
              formatter.format(u.start()),
              formatter.format(u.end()),
              u.reason() == null ? "" : u.reason()
            });
      }
    }

    private void addUnavailability() {
      String startRaw =
          JOptionPane.showInputDialog(
              this,
              "Début (AAAA-MM-JJ HH:MM)",
              LocalDateTime.now().withSecond(0).withNano(0).toString().replace('T', ' '));
      if (startRaw == null) {
        return;
      }
      String endRaw =
          JOptionPane.showInputDialog(
              this,
              "Fin (AAAA-MM-JJ HH:MM)",
              LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).toString().replace('T', ' '));
      if (endRaw == null) {
        return;
      }
      LocalDateTime start;
      LocalDateTime end;
      try {
        start = parseDateTime(startRaw);
        end = parseDateTime(endRaw);
      } catch (DateTimeParseException ex) {
        JOptionPane.showMessageDialog(this, "Format de date invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        return;
      }
      if (!end.isAfter(start)) {
        JOptionPane.showMessageDialog(
            this, "La date de fin doit être postérieure au début", "Erreur", JOptionPane.ERROR_MESSAGE);
        return;
      }
      String reason =
          JOptionPane.showInputDialog(this, "Motif", "Indisponibilité", JOptionPane.QUESTION_MESSAGE);
      if (reason == null) {
        return;
      }
      Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
      Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();
      try {
        dataSourceProvider.saveUnavailability(
            new Models.Unavailability(null, resource.id(), reason, startInstant, endInstant, false));
        loadUnavailabilities();
        Toasts.success(SwingUtilities.getWindowAncestor(this), "Indisponibilité ajoutée");
      } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      }
    }

    private LocalDateTime parseDateTime(String raw) {
      String normalized = raw.trim().replace('T', ' ');
      return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void deleteUnavailability() {
      int row = unavailabilityTable.getSelectedRow();
      if (row < 0 || row >= unavailabilityCache.size()) {
        return;
      }
      Models.Unavailability selected = unavailabilityCache.get(row);
      try {
        dataSourceProvider.deleteUnavailability(selected.id());
        loadUnavailabilities();
        Toasts.info(SwingUtilities.getWindowAncestor(this), "Indisponibilité supprimée");
      } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      }
    }

    private void chooseColor() {
      Color initial = colorRgb == null ? new Color(0x5096FF) : new Color(colorRgb);
      Color chosen = javax.swing.JColorChooser.showDialog(this, "Couleur", initial);
      if (chosen != null) {
        colorRgb = chosen.getRGB() & 0xFFFFFF;
        updateColorButton();
      }
    }

    private void updateColorButton() {
      Color display = colorRgb == null ? new Color(0x5096FF) : new Color(colorRgb);
      btColor.setBackground(display);
      btColor.setForeground(display.getRGB() < 0x7F7F7F ? Color.WHITE : Color.BLACK);
      btColor.setOpaque(true);
    }

    private void updateTypeIcon() {
      Models.ResourceType selected = (Models.ResourceType) cbType.getSelectedItem();
      String iconName = selected == null ? fallbackType.iconName() : selected.iconName();
      lbTypeIcon.setIcon(SvgIconLoader.load(iconName, 32));
    }

    private void save() {
      String name = tfName.getText().trim();
      if (name.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nom obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
        return;
      }
      String plate = tfPlate.getText().trim();
      String tags = tfTags.getText().trim();
      Integer capacity = (Integer) spCapacity.getValue();
      if (capacity != null && capacity == 0) {
        capacity = null;
      }
      Double rateValue = null;
      if (rateSupported) {
        String rateRaw = tfRate.getText().trim().replace(',', '.');
        if (!rateRaw.isEmpty()) {
          try {
            rateValue = Double.parseDouble(rateRaw);
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this, "Prix invalide", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
          }
        }
      }

      Models.Resource updated =
          new Models.Resource(
              resource.id(),
              name,
              plate,
              colorRgb,
              resource.agencyId(),
              tags,
              capacity);
      try {
        resource = dataSourceProvider.saveResource(updated);
        if (rateSupported) {
          double effectiveRate = rateValue == null ? 0.0 : rateValue;
          dataSourceProvider.setResourceDailyRate(resource.id(), effectiveRate);
        }
        if (typesSupported) {
          Models.ResourceType selected = (Models.ResourceType) cbType.getSelectedItem();
          String typeId = selected == null ? null : selected.id();
          dataSourceProvider.setResourceTypeForResource(resource.id(), typeId);
        }
        if (callback != null) {
          callback.accept(resource);
        }
      } catch (UnsupportedOperationException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      } catch (RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
