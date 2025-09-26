package com.location.client.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

public final class ImageExport {
  private ImageExport() {}

  public static void exportComponent(JComponent component, Path target) throws IOException {
    int width = Math.max(1, component.getWidth());
    int height = Math.max(1, component.getHeight());
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      component.printAll(graphics);
    } finally {
      graphics.dispose();
    }
    ImageIO.write(image, "png", target.toFile());
  }
}
