/*
 *  Copyright 2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.scala.magic.command;

import com.twosigma.beakerx.evaluator.SimpleEvaluationObjectFactory;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.magic.command.CodeFactory;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandExecutionParam;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandFunctionality;
import com.twosigma.beakerx.kernel.magic.command.functionality.ClasspathAddJarMagicCommand;
import com.twosigma.beakerx.kernel.magic.command.functionality.ClasspathAddJarMagicCommandInfo;
import com.twosigma.beakerx.kernel.magic.command.functionality.LoadMagicMagicCommand;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutcomeItem;
import com.twosigma.beakerx.kernel.msg.MessageCreator;
import com.twosigma.beakerx.util.BeakerXSystemImpl;

import java.util.Optional;

public class SparkInitCommandFactoryImpl implements SparkInitCommandFactory {

  private KernelFunctionality kernel;
  private SparkexJarService sparkexJarService;
  private final EnableSparkSupportOptions enableSparkSupportOptions;
  private final EnableSparkSupportActionOptions supportActionOptions;
  private final CodeFactory codeFactory;

  public SparkInitCommandFactoryImpl(KernelFunctionality kernel,
                                     SparkexJarService sparkexJarService,
                                     EnableSparkSupportOptions enableSparkSupportOptions,
                                     EnableSparkSupportActionOptions supportActionOptions) {
    this.kernel = kernel;
    this.sparkexJarService = sparkexJarService;
    this.enableSparkSupportOptions = enableSparkSupportOptions;
    this.supportActionOptions = supportActionOptions;
    this.codeFactory = new CodeFactory(MessageCreator.get(), new SimpleEvaluationObjectFactory());
  }

  @Override
  public Command addSparkexJar() {
    return new Command() {
      @Override
      public MagicCommandOutcomeItem run() {
        return sparkexJarService.addSparkexJar(kernel);
      }

      @Override
      public String getErrorMessage() {
        return "Cannot load sparkex.jar";
      }
    };
  }

  @Override
  public Command loadSparkSupportMagicClass() {
    return new LoadSparkSupportMagicClassCommand(this.codeFactory);
  }

  private class LoadSparkSupportMagicClassCommand implements Command {
    private CodeFactory codeFactory;

    public LoadSparkSupportMagicClassCommand(CodeFactory codeFactory) {
      this.codeFactory = codeFactory;
    }

    @Override
    public MagicCommandOutcomeItem run() {
      Optional<MagicCommandFunctionality> magic = this.codeFactory.findMagicCommandFunctionality(kernel.getMagicCommandTypes(), LoadMagicMagicCommand.LOAD_MAGIC);
      MagicCommandOutcomeItem magicCommandOutcomeItem = ((LoadMagicMagicCommand) magic.get())
              .load("com.twosigma.beakerx.scala.magic.command.LoadSparkSupportMagicCommand");
      return magicCommandOutcomeItem;
    }

    @Override
    public String getErrorMessage() {
      return "Cannot load LoadSparkSupportMagicCommand class";
    }
  }

  @Override
  public Command runOptions(MagicCommandExecutionParam param) {
    return new RunOptionsCommand(enableSparkSupportOptions, param.getCommand(), param.getCode().getMessage());
  }

  @Override
  public Command loadSparkFrom_SPARK_HOME_IfIsNotOnClasspath() {
    Optional<MagicCommandFunctionality> magic = this.codeFactory.findMagicCommandFunctionality(kernel.getMagicCommandTypes(), ClasspathAddJarMagicCommandInfo.CLASSPATH_ADD_JAR);
    ClasspathAddJarMagicCommand classpathAddJarMagicCommand = (ClasspathAddJarMagicCommand) magic.get();
    return new LoadSparkFrom_SPARK_HOME_Command(classpathAddJarMagicCommand, kernel.getClasspath(), BeakerXSystemImpl.getINSTANCE());
  }

  @Override
  public Command loadLatestVersionOfSparkIfIsNotOnClasspath(MagicCommandExecutionParam param) {
    return new LoadLatestVersionOfSparkIfIsNotOnClasspath(kernel.getClasspath(), supportActionOptions, param.getCode().getMessage());
  }

  @Override
  public Command loadSparkSupportMagic(MagicCommandExecutionParam param) {
    return new LoadSparkSupportMagicCommand(param, this.codeFactory);
  }

  private class LoadSparkSupportMagicCommand implements Command {
    private final MagicCommandExecutionParam param;
    private CodeFactory codeFactory;

    public LoadSparkSupportMagicCommand(MagicCommandExecutionParam param, CodeFactory codeFactory) {
      this.param = param;
      this.codeFactory = codeFactory;
    }

    @Override
    public MagicCommandOutcomeItem run() {
      String loadSparkMagic = "%loadSparkSupport";
      Optional<MagicCommandFunctionality> magic = this.codeFactory.findMagicCommandFunctionality(kernel.getMagicCommandTypes(), loadSparkMagic);
      MagicCommandOutcomeItem execute = magic.get()
              .execute(param);
      return execute;
    }

    @Override
    public String getErrorMessage() {
      return "Error loading Spark, was it added to the classpath?";
    }
  }

}
