package jetbrains.buildServer.runAs.agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.dotNet.buildRunner.agent.*;
import jetbrains.buildServer.runAs.common.Constants;
import jetbrains.buildServer.runAs.common.RunAsMode;
import jetbrains.buildServer.runAs.common.LoggingLevel;
import jetbrains.buildServer.runAs.common.WindowsIntegrityLevel;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

public class UserCredentialsServiceTest {
  private final File myAgentHomeDir;
  private final File myRunAsCredDir;
  private final File myUser2Cred;
  private final File myDefaultCred;
  private final File myAgentBinDir;
  private Mockery myCtx;
  private ParametersService myParametersService;
  private PropertiesService myPropertiesService;
  private BuildAgentConfiguration myBuildAgentConfiguration;
  private CommandLineArgumentsService myCommandLineArgumentsService;

  public UserCredentialsServiceTest() {
    myAgentHomeDir = new File("homeDir");
    myAgentBinDir = new File(myAgentHomeDir, "bin");
    myRunAsCredDir = new File(myAgentBinDir, "RunAsCredDir");
    myUser2Cred = new File(myRunAsCredDir, "user2cred.properties");
    myDefaultCred = new File(myRunAsCredDir, UserCredentialsServiceImpl.DEFAULT_CREDENTIALS + ".properties");
  }

  @BeforeMethod
  public void setUp()
  {
    myCtx = new Mockery();
    myParametersService = myCtx.mock(ParametersService.class);
    myPropertiesService = myCtx.mock(PropertiesService.class);
    myBuildAgentConfiguration = myCtx.mock(BuildAgentConfiguration.class);
    myCommandLineArgumentsService = myCtx.mock(CommandLineArgumentsService.class);
  }

  @DataProvider(name = "getUserCredentialsCases")
  public Object[][] getUserCredentialsCases() {
    return new Object[][] {
      // Default && ret null
      {
        new HashMap<String, String>(),
        new HashMap<String, String>(),
        new HashMap<String, String>(),
        new VirtualFileService(),
        null,
        null
      },

      // Predefined && ret null
      {
        new HashMap<String, String>(),
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
        }},
        new HashMap<String, String>(),
        new VirtualFileService(),
        null,
        "Configuration parameter \"" + Constants.CREDENTIALS_DIRECTORY + "\" was not defined"
      },

      // PredefinedCredentials && predefined credentials
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        new UserCredentials("user2", "password2", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // PredefinedCredentials && default credentials
      {
        new HashMap<String, String>(),
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user3");
          put(Constants.PASSWORD, "password3");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myDefaultCred, "")),
        new UserCredentials("user3", "password3", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // PredefinedCredentials && predefined credentials && throw exception WHEN password is empty
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Password must be defined"
      },

      // PredefinedCredentials && predefined credentials && throw exception WHEN password is null
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Password must be defined"
      },

      // PredefinedCredentials && predefined credentials WHEN throw exception when user is empty
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Username must be defined"
      },

      // PredefinedCredentials && predefined credentials WHEN throw exception when user is null
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Username must be defined"
      },

      // PredefinedCredentials && predefined credentials WHEN cred file is directory
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualDirectory(myUser2Cred)),
        null,
        "Credentials file .* was not found"
      },

      // PredefinedCredentials && predefined credentials WHEN there is not cred file
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir)),
        null,
        "Credentials file .* was not found"
      },

      // PredefinedCredentials && predefined credentials WHEN cred dir is not a dir
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualFile(myRunAsCredDir, "")),
        null,
        "Credentials directory was not found"
      },

      // PredefinedCredentials && predefined credentials WHEN cred dir does not exist
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(),
        null,
        "Credentials directory was not found"
      },

      // PredefinedCredentials && predefined credentials WHEN CREDENTIALS_DIRECTORY is not defined as configuration parameter
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS, "user2cred");
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(),
        null,
        "Configuration parameter \"" + Constants.CREDENTIALS_DIRECTORY + "\" was not defined"
      },

      // PredefinedCredentials && predefined credentials WHEN CREDENTIALS is not defined as configuration parameter
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(),
        null,
        "Configuration parameter \"" + Constants.CREDENTIALS_DIRECTORY + "\" was not defined"
      },

      // PredefinedCredentials && use default credentials
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user4");
          put(Constants.PASSWORD, "password4");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myDefaultCred, "")
        ),
        new UserCredentials("user4", "password4", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // Enabled by default && custom credentials
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1"); }},
        new HashMap<String, String>(),
        new HashMap<String, String>(),
        new VirtualFileService(),
        new UserCredentials("user1", "password1", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // Enabled && custom credentials
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1"); }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue()); }},
        new HashMap<String, String>(),
        new VirtualFileService(),
        new UserCredentials("user1", "password1", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // Enabled && predefined credentials WHEN custom credentials is not defined
      {
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS, "user2cred");}},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        new UserCredentials("user2", "password2", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // Enabled && predefined credentials WHEN custom credentials is not defined
      {
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS, "user10000cred");}},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Credentials file for .* was not found"
      },

      // Enabled && predefined credentials && ret null WHEN credentials is not defined at all
      {
        new HashMap<String, String>(),
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user55");
          put(Constants.PASSWORD, "password55");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        null
      },

      // PredefinedCredentials && ignore custom credentials
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1"); }},
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName()); }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        new UserCredentials("user2", "password2", WindowsIntegrityLevel.Auto, LoggingLevel.Off, Arrays.<CommandLineArgument>asList()),
        null
      },

      // PredefinedCredentials && custom credentials && try to use default credentials and throw an exception WHEN CREDENTIALS is not defined as configuration parameter
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user34");
          put(Constants.PASSWORD, "password34"); }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.PredefinedCredentials.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName()); }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user35");
          put(Constants.PASSWORD, "password35");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        "Credentials file for .+ was not found"
      },

      // Disabled && ret null
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Disabled.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user66");
          put(Constants.PASSWORD, "password66");
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        null,
        null
      },

      // Disabled && ret null
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user99");
          put(Constants.PASSWORD, "password99");
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Disabled.getValue());
          put(Constants.CREDENTIALS, "user2cred");
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>(),
        new VirtualFileService(),
        null,
        null
      },

      // Enabled && custom credentials && additional params
      {
        new HashMap<String, String>() {{
          put(Constants.USER, "user1");
          put(Constants.PASSWORD, "password1");
          put(Constants.ADDITIONAL_ARGS, "arg1 arg2");
          put(Constants.WINDOWS_INTEGRITY_LEVEL, WindowsIntegrityLevel.High.getValue());
          put(Constants.WINDOWS_LOGGING_LEVEL, LoggingLevel.Debug.getValue());
        }},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue());
        }},
        new HashMap<String, String>(),
        new VirtualFileService(),
        new UserCredentials("user1", "password1", WindowsIntegrityLevel.High, LoggingLevel.Debug, Arrays.asList(new CommandLineArgument("arg1", CommandLineArgument.Type.PARAMETER), new CommandLineArgument("arg2", CommandLineArgument.Type.PARAMETER))),
        null
      },

      // Enabled && predefined credentials && additional params WHEN custom credentials is not defined
      {
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS, "user2cred");}},
        new HashMap<String, String>() {{
          put(Constants.RUN_AS_MODE, RunAsMode.Enabled.getValue());
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user2");
          put(Constants.PASSWORD, "password2");
          put(Constants.ADDITIONAL_ARGS, "arg1 arg2");
          put(Constants.WINDOWS_INTEGRITY_LEVEL, WindowsIntegrityLevel.High.getValue());
          put(Constants.WINDOWS_LOGGING_LEVEL, LoggingLevel.Debug.getValue());
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myUser2Cred, "")),
        new UserCredentials("user2", "password2", WindowsIntegrityLevel.High, LoggingLevel.Debug, Arrays.asList(new CommandLineArgument("arg1", CommandLineArgument.Type.PARAMETER), new CommandLineArgument("arg2", CommandLineArgument.Type.PARAMETER))),
        null
      },

      // Enabled && default credentials
      {
        new HashMap<String, String>(),
        new HashMap<String, String>() {{
          put(Constants.CREDENTIALS_DIRECTORY, myRunAsCredDir.getName());
        }},
        new HashMap<String, String>() {{
          put(Constants.USER, "user78");
          put(Constants.PASSWORD, "password78");
          put(Constants.ADDITIONAL_ARGS, "arg1 arg2");
          put(Constants.WINDOWS_INTEGRITY_LEVEL, WindowsIntegrityLevel.High.getValue());
          put(Constants.WINDOWS_LOGGING_LEVEL, LoggingLevel.Debug.getValue());
        }},
        new VirtualFileService(
          new VirtualFileService.VirtualDirectory(myRunAsCredDir),
          new VirtualFileService.VirtualFile(myDefaultCred, "")),
        new UserCredentials("user78", "password78", WindowsIntegrityLevel.High, LoggingLevel.Debug, Arrays.asList(new CommandLineArgument("arg1", CommandLineArgument.Type.PARAMETER), new CommandLineArgument("arg2", CommandLineArgument.Type.PARAMETER))),
        null
      },
    };
  }

  @Test(dataProvider = "getUserCredentialsCases")
  public void shouldGetUserCredentials(
    @NotNull final HashMap<String, String> parameters,
    @NotNull final HashMap<String, String> configParameters,
    @NotNull final HashMap<String, String> properties,
    @NotNull final VirtualFileService fileService,
    @Nullable final UserCredentials expectedUserCredentials,
    @Nullable final String expectedExceptionMessage) throws IOException {
    // Given
    myCtx.checking(new Expectations() {{
      allowing(myBuildAgentConfiguration).getAgentHomeDirectory();
      will(returnValue(myAgentHomeDir));

      allowing(myParametersService).tryGetParameter(with(any(String.class)));
      will(new CustomAction("tryGetParameter") {
        @Override
        public Object invoke(final Invocation invocation) throws Throwable {
          final String name = (String)invocation.getParameter(0);
          return parameters.get(name);
        }
      });

      allowing(myParametersService).tryGetConfigParameter(with(any(String.class)));
      will(new CustomAction("tryGetConfigParameter") {
        @Override
        public Object invoke(final Invocation invocation) throws Throwable {
          final String name = (String)invocation.getParameter(0);
          return configParameters.get(name);
        }
      });

      allowing(myPropertiesService).tryGetProperty(with(any(String.class)));
      will(new CustomAction("tryGetProperty") {
        @Override
        public Object invoke(final Invocation invocation) throws Throwable {
          final String name = (String)invocation.getParameter(0);
          return properties.get(name);
        }
      });

      allowing(myPropertiesService).load(with(any(File.class)));

      allowing(myCommandLineArgumentsService).parseCommandLineArguments(with(any(String.class)));
      will(new CustomAction("parseCommandLineArguments") {
        @Override
        public Object invoke(final Invocation invocation) throws Throwable {
          final ArrayList<CommandLineArgument> args = new ArrayList<CommandLineArgument>();
          for(String arg: StringUtil.split((String)invocation.getParameter(0))) {
            args.add(new CommandLineArgument(arg, CommandLineArgument.Type.PARAMETER));
          }

          return args;
        }
      });
    }});

    final UserCredentialsService userCredentialsService = createInstance(fileService);
    BuildStartException actualException = null;

    // When
    UserCredentials actualUserCredentials = null;
    try {
      actualUserCredentials = userCredentialsService.tryGetUserCredentials();
    }
    catch (BuildStartException ex) {
      actualException = ex;
    }

    // Then
    myCtx.assertIsSatisfied();
    then(actualException != null).isEqualTo(expectedExceptionMessage != null);
    if(actualException != null && expectedExceptionMessage != null) {
      Pattern pattern = Pattern.compile(expectedExceptionMessage);
      then(pattern.matcher(actualException.getMessage()).find()).isEqualTo(true);
    }

    then(actualUserCredentials).isEqualTo(expectedUserCredentials);
  }

  @NotNull
  private UserCredentialsService createInstance(FileService fileService)
  {
    return new UserCredentialsServiceImpl(
      myParametersService,
      myPropertiesService,
      fileService,
      myBuildAgentConfiguration,
      myCommandLineArgumentsService);
  }
}