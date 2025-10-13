package com.combostrap.docExec;



import java.util.logging.Logger;

public class DocLog {

  public final static Logger LOGGER = Logger.getLogger(DocLog.class.getPackageName());


  public static String onOneLine(String string) {
    return string.replaceAll("\r\n|\n", " ") // No new line
            .replaceAll(" ", " ");
  }

}
