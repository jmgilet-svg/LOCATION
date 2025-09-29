package com.location.client.ui.icons;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

public final class SvgIconLoader {
  private static final String ICONS_BASE = "/icons/";

  private SvgIconLoader() {}

  public static ImageIcon load(String iconName, int size) {
    if (iconName == null || iconName.isBlank()) {
      return null;
    }
    try (InputStream in = SvgIconLoader.class.getResourceAsStream(ICONS_BASE + iconName)) {
      if (in == null) {
        return null;
      }
      BufferedImage img = rasterize(in, size, size);
      return new ImageIcon(img);
    } catch (IOException | TranscoderException ex) {
      return null;
    }
  }

  private static BufferedImage rasterize(InputStream in, int width, int height) throws TranscoderException {
    class BufferedImageTranscoderImpl extends ImageTranscoder {
      private BufferedImage image;

      @Override
      public BufferedImage createImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      }

      @Override
      public void writeImage(BufferedImage img, TranscoderOutput out) {
        this.image = img;
      }

      BufferedImage getImage() {
        return image;
      }
    }

    BufferedImageTranscoderImpl transcoder = new BufferedImageTranscoderImpl();
    transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
    transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
    transcoder.transcode(new TranscoderInput(in), null);
    return transcoder.getImage();
  }

  public static List<String> listAvailable() {
    try (InputStream in = SvgIconLoader.class.getResourceAsStream(ICONS_BASE + "icons.txt")) {
      if (in == null) {
        return defaultIcons();
      }
      String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      List<String> icons = new ArrayList<>();
      for (String line : content.split("\n")) {
        String trimmed = line.trim();
        if (!trimmed.isBlank()) {
          icons.add(trimmed);
        }
      }
      return icons.isEmpty() ? defaultIcons() : icons;
    } catch (IOException ex) {
      return defaultIcons();
    }
  }

  private static List<String> defaultIcons() {
    return List.of(
        "crane.svg",
        "truck.svg",
        "trailer.svg",
        "forklift.svg",
        "excavator.svg",
        "hook.svg",
        "driver.svg",
        "nacelle.svg",
        "conflict.svg");
  }
}
