package com.combostrap.docExec.util;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;


public class JarManifest {


  private final HashMap<String,String> attributes = new HashMap<>();


  /**
   * @param aClazz the class from which the manifest should be found
   */
  public JarManifest(Class<?> aClazz) throws JarManifestNotFoundException {

    String name = aClazz.getSimpleName() + ".class";
    URL resourceUrl = aClazz.getResource(name);
    if (resourceUrl == null) {
      throw new JarManifestNotFoundException();
    }
    String resourceUrlAsString = resourceUrl.toString();
    if (!resourceUrlAsString.startsWith("jar")) {
      // Class not from JAR
      throw new JarManifestNotFoundException();
    }

    String manifestPath = resourceUrlAsString.substring(0, resourceUrlAsString.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
    URL url;
    try {
      url = new URL(manifestPath);
    } catch (IOException e) {
      throw new InternalError("Error, the manifest path (" + manifestPath + ") is not an URL", e);
    }

    try (InputStream is = url.openStream()) {
      java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
      for (Map.Entry<Object, Object> attribute : manifest.getMainAttributes().entrySet()) {
        attributes.put(String.valueOf(attribute.getKey()), String.valueOf(attribute.getValue()));
      }
      for (Map.Entry<String, Attributes> manifestEntry : manifest.getEntries().entrySet()) {
        Attributes localAttributes = manifestEntry.getValue();
        for (Map.Entry<Object, Object> attribute : localAttributes.entrySet()) {
          attributes.put(String.valueOf(attribute.getKey()), String.valueOf(attribute.getValue()));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

  }

  public static JarManifest createFor(Class<?> aClazz) throws JarManifestNotFoundException {
    return new JarManifest(aClazz);
  }

  public Map<String, String> getMap() {
    return this.attributes;
  }

  public String getAttributeValue(String key) {
    return this.attributes.get(key);
  }


}
