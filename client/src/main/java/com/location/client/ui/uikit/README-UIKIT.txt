UI Kit Starter for LOCATION
- Theme.applyLight()/applyDark() use FlatLaf if present (reflection), fallback to System LAF
- Add dependencies to enable SVG & FlatLaf:

<dependency>
  <groupId>com.formdev</groupId>
  <artifactId>flatlaf</artifactId>
  <version>3.4.1</version>
</dependency>
<dependency>
  <groupId>com.formdev</groupId>
  <artifactId>flatlaf-extras</artifactId>
  <version>3.4.1</version>
</dependency>

- Put SVGs in client/src/main/resources/icons
- Optional fonts in client/src/main/resources/fonts (Inter-*.ttf)
