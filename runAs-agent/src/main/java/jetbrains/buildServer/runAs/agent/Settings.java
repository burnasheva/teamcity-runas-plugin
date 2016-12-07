package jetbrains.buildServer.runAs.agent;

import java.util.List;
import jetbrains.buildServer.dotNet.buildRunner.agent.CommandLineArgument;
import jetbrains.buildServer.runAs.common.LoggingLevel;
import jetbrains.buildServer.runAs.common.WindowsIntegrityLevel;
import org.jetbrains.annotations.NotNull;

public class Settings {
  private final String myUser;
  private final String myPassword;
  private final WindowsIntegrityLevel myWindowsIntegrityLevel;
  private final LoggingLevel myWindowsLoggingLevel;
  private final List<CommandLineArgument> myAdditionalArgs;

  Settings(
    @NotNull final String user,
    @NotNull final String password,
    @NotNull final WindowsIntegrityLevel windowsIntegrityLevel,
    @NotNull final LoggingLevel windowsLoggingLevel,
    @NotNull final List<CommandLineArgument> additionalArgs) {
    myUser = user;
    myPassword = password;
    myWindowsIntegrityLevel = windowsIntegrityLevel;
    myWindowsLoggingLevel = windowsLoggingLevel;
    myAdditionalArgs = additionalArgs;
  }

  @NotNull
  String getUser() {
    return myUser;
  }

  @NotNull
  String getPassword() {
    return myPassword;
  }

  public WindowsIntegrityLevel getWindowsIntegrityLevel() {
    return myWindowsIntegrityLevel;
  }

  public LoggingLevel getWindowsLoggingLevel() {
    return myWindowsLoggingLevel;
  }

  @NotNull
  List<CommandLineArgument> getAdditionalArgs() {
    return myAdditionalArgs;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Settings settings = (Settings)o;

    if (!myUser.equals(settings.myUser)) return false;
    if (!myPassword.equals(settings.myPassword)) return false;
    if (myWindowsIntegrityLevel != settings.myWindowsIntegrityLevel) return false;
    if (myWindowsLoggingLevel != settings.myWindowsLoggingLevel) return false;
    return myAdditionalArgs.equals(settings.myAdditionalArgs);

  }

  @Override
  public int hashCode() {
    int result = myUser.hashCode();
    result = 31 * result + myPassword.hashCode();
    result = 31 * result + myWindowsIntegrityLevel.hashCode();
    result = 31 * result + myWindowsLoggingLevel.hashCode();
    result = 31 * result + myAdditionalArgs.hashCode();
    return result;
  }
}
