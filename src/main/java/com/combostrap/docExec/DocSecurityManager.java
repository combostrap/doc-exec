package com.combostrap.docExec;

import java.security.Permission;

/**
 * Security Manager that catch all `System.exit` that occurs during code unit execution
 * and throws them
 * For instance, a java code with System or help
 * Doc: <a href="https://stackoverflow.com/questions/5549720/how-to-prevent-calls-to-system-exit-from-terminating-the-jvm">how-to-prevent-calls-to-system-exit-from-terminating-the-jvm</a>
 */
public class DocSecurityManager extends SecurityManager {

  private static DocSecurityManager docSecurityManager;
  private boolean isRunning = false;


  public static DocSecurityManager create() {
    if (docSecurityManager == null) {
      docSecurityManager = new DocSecurityManager();
    }
    return docSecurityManager;
  }

  @Override
  public void checkExit(int status) {

    if (this.isRunning) {
      // Doing nothing means that the JVM will exit
      // throwing is the only way to prevent an exit in case of error
      throw new DocExitStatusException(status);
    }

  }

  @Override
  public void checkPermission(Permission perm) {
    // Allow other activities by default
  }

  /**
   * @param isRunning - indicate if the eval or exec is running and we need to prevent an exit
   */
  public void setCodeIsRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }
}
