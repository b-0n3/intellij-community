// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.console;

import com.google.common.collect.Maps;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathMapper;
import com.jetbrains.python.buildout.BuildoutFacet;
import com.jetbrains.python.run.EnvironmentController;
import com.jetbrains.python.run.PlainEnvironmentController;
import com.jetbrains.python.run.PythonCommandLineState;
import com.jetbrains.python.run.PythonRunConfiguration;
import com.jetbrains.python.sdk.PythonEnvUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PydevConsoleRunnerFactory extends PythonConsoleRunnerFactory {

  protected static class ConsoleParameters {
    @NotNull Project myProject;
    @Nullable Sdk mySdk;
    @Nullable String myWorkingDir;
    @NotNull Map<String, String> myEnvs;
    @NotNull PyConsoleType myConsoleType;
    @NotNull PyConsoleOptions.PyConsoleSettings mySettingsProvider;
    String @NotNull [] mySetupFragment;

    public ConsoleParameters(@NotNull Project project,
                             @Nullable Sdk sdk,
                             @Nullable String workingDir,
                             @NotNull Map<String, String> envs,
                             @NotNull PyConsoleType consoleType,
                             @NotNull PyConsoleOptions.PyConsoleSettings settingsProvider,
                             String @NotNull [] setupFragment) {
      myProject = project;
      mySdk = sdk;
      myWorkingDir = workingDir;
      myEnvs = envs;
      myConsoleType = consoleType;
      mySettingsProvider = settingsProvider;
      mySetupFragment = setupFragment;
    }

    public @NotNull Project getProject() {
      return myProject;
    }

    public @Nullable Sdk getSdk() {
      return mySdk;
    }

    public @Nullable String getWorkingDir() {
      return myWorkingDir;
    }

    public @NotNull Map<String, String> getEnvs() {
      return myEnvs;
    }

    public @NotNull PyConsoleType getConsoleType() {
      return myConsoleType;
    }

    public PyConsoleOptions.@NotNull PyConsoleSettings getSettingsProvider() {
      return mySettingsProvider;
    }

    public String[] getSetupFragment() {
      return mySetupFragment;
    }
  }

  protected ConsoleParameters createConsoleParameters(@NotNull Project project,
                                                      @Nullable Module contextModule) {
    Pair<Sdk, Module> sdkAndModule = PydevConsoleRunner.findPythonSdkAndModule(project, contextModule);

    @Nullable Module module = sdkAndModule.second;

    @Nullable Sdk sdk = sdkAndModule.first;

    PyConsoleOptions.PyConsoleSettings settingsProvider = PyConsoleOptions.getInstance(project).getPythonConsoleSettings();

    PathMapper pathMapper = PydevConsoleRunner.getPathMapper(project, sdk, settingsProvider);

    String workingDir = getWorkingDir(project, module, pathMapper, settingsProvider);

    String[] setupFragment = createSetupFragment(module, workingDir, pathMapper, settingsProvider);

    Map<String, String> envs = Maps.newHashMap(settingsProvider.getEnvs());
    putIPythonEnvFlag(project, envs);

    return new ConsoleParameters(project, sdk, workingDir, envs, PyConsoleType.PYTHON, settingsProvider, setupFragment);
  }

  @Override
  @NotNull
  public PydevConsoleRunner createConsoleRunner(@NotNull Project project,
                                                @Nullable Module contextModule) {
    final ConsoleParameters consoleParameters = createConsoleParameters(project, contextModule);
    return createConsoleRunner(project, consoleParameters.mySdk, consoleParameters.myWorkingDir, consoleParameters.myEnvs,
                               consoleParameters.myConsoleType,
                               consoleParameters.mySettingsProvider, consoleParameters.mySetupFragment);
  }

  public static void putIPythonEnvFlag(@NotNull Project project, @NotNull Map<String, String> envs) {
    putIPythonEnvFlag(project, new PlainEnvironmentController(envs));
  }

  public static void putIPythonEnvFlag(@NotNull Project project, @NotNull EnvironmentController environmentController) {
    String ipythonEnabled = PyConsoleOptions.getInstance(project).isIpythonEnabled() ? "True" : "False";
    environmentController.putFixedValue(PythonEnvUtil.IPYTHONENABLE, ipythonEnabled);
  }

  @Nullable
  public static String getWorkingDir(@NotNull Project project,
                                     @Nullable Module module,
                                     @Nullable PathMapper pathMapper,
                                     PyConsoleOptions.PyConsoleSettings settingsProvider) {
    String workingDir = settingsProvider.getWorkingDirectory();
    if (StringUtil.isEmpty(workingDir)) {
      if (module != null && ModuleRootManager.getInstance(module).getContentRoots().length > 0) {
        workingDir = ModuleRootManager.getInstance(module).getContentRoots()[0].getPath();
      }
      else {
        VirtualFile[] projectRoots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile root: projectRoots) {
          if (root.getFileSystem() instanceof LocalFileSystem) {
            // we can't start Python Console in remote folder without additional connection configurations
            workingDir = root.getPath();
            break;
          }
        }
      }
    }
    if (workingDir.isEmpty()) {
      workingDir = System.getProperty("user.home");
    }

    if (pathMapper != null && workingDir != null) {
      workingDir = pathMapper.convertToRemote(workingDir);
    }

    return workingDir;
  }

  public static String[] createSetupFragment(@Nullable Module module,
                                             @Nullable String workingDir,
                                             @Nullable PathMapper pathMapper,
                                             PyConsoleOptions.PyConsoleSettings settingsProvider) {
    String customStartScript = settingsProvider.getCustomStartScript();
    if (customStartScript.trim().length() > 0) {
      customStartScript = "\n" + customStartScript;
    }
    Collection<String> pythonPath = PythonCommandLineState.collectPythonPath(module, settingsProvider.shouldAddContentRoots(),
                                                                             settingsProvider.shouldAddSourceRoots());
    if (pathMapper != null) {
      pythonPath = pathMapper.convertToRemote(pythonPath);
    }
    String selfPathAppend = PydevConsoleRunner.constructPyPathAndWorkingDirCommand(pythonPath, workingDir, customStartScript);

    BuildoutFacet facet = null;
    if (module != null) {
      facet = BuildoutFacet.getInstance(module);
    }
    String[] setupFragment;
    if (facet != null) {
      List<String> path = facet.getAdditionalPythonPath();
      if (pathMapper != null) {
        path = pathMapper.convertToRemote(path);
      }
      String prependStatement = facet.getPathPrependStatement(path);
      setupFragment = new String[]{prependStatement, selfPathAppend};
    }
    else {
      setupFragment = new String[]{selfPathAppend};
    }

    return setupFragment;
  }

  @NotNull
  protected PydevConsoleRunner createConsoleRunner(@NotNull Project project,
                                                   @Nullable Sdk sdk,
                                                   @Nullable String workingDir,
                                                   @NotNull Map<String, String> envs,
                                                   @NotNull PyConsoleType consoleType,
                                                   @NotNull PyConsoleOptions.PyConsoleSettings settingsProvider,
                                                   String @NotNull ... setupFragment) {
    return new PydevConsoleRunnerImpl(project, sdk, consoleType, workingDir, envs, settingsProvider, setupFragment);
  }

  @Override
  @NotNull
  public PydevConsoleRunner createConsoleRunnerWithFile(@NotNull Project project,
                                                        @Nullable Module contextModule,
                                                        @Nullable String runFileText,
                                                        @NotNull PythonRunConfiguration config) {
    final ConsoleParameters consoleParameters = createConsoleParameters(project, contextModule);
    Sdk sdk = config.getSdk() != null ? config.getSdk() : consoleParameters.mySdk;
    String workingDir = config.getWorkingDirectory() != null ? config.getWorkingDirectory() : consoleParameters.myWorkingDir;

    Map<String, String> consoleEnvs = new HashMap<>();
    consoleEnvs.putAll(consoleParameters.myEnvs);
    consoleEnvs.putAll(config.getEnvs());

    return new PydevConsoleWithFileRunnerImpl(project, sdk, consoleParameters.myConsoleType, config.getName(), workingDir,
                                              consoleEnvs, consoleParameters.mySettingsProvider, config, consoleParameters.mySetupFragment);
  }
}
