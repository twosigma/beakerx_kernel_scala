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

import com.twosigma.beakerx.Display;
import com.twosigma.beakerx.TryResult;
import com.twosigma.beakerx.evaluator.InternalVariable;
import com.twosigma.beakerx.evaluator.SimpleEvaluationObjectFactory;
import com.twosigma.beakerx.jvm.object.EvaluationObject;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandExecutionParam;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutcomeItem;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutput;
import com.twosigma.beakerx.kernel.msg.MessageCreator;
import com.twosigma.beakerx.message.Message;
import com.twosigma.beakerx.scala.magic.command.SparkMagicCommandOptions.SparkOptionCommand;
import com.twosigma.beakerx.widget.SparkEngineNoUI;
import com.twosigma.beakerx.widget.SparkEngineNoUIImpl.SparkEngineNoUIFactory;
import com.twosigma.beakerx.widget.SparkEngineWithUI;
import com.twosigma.beakerx.widget.SparkEngineWithUIFactory;
import com.twosigma.beakerx.widget.SparkListenerService;
import com.twosigma.beakerx.widget.SparkSessionBuilder;
import com.twosigma.beakerx.widget.SparkSessionBuilderFactory;
import com.twosigma.beakerx.widget.SparkUIApi;
import com.twosigma.beakerx.widget.SparkUIFactory;
import com.twosigma.beakerx.widget.SparkUiDefaults;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

import java.util.List;
import java.util.Optional;

import static com.twosigma.beakerx.scala.magic.command.EnableSparkSupportMagicCommand.ENABLE_SPARK_SUPPORT;
import static java.util.Optional.of;

public class SparkFactoryImpl implements SparkFactory {

  public static final String SPARK_SESSION_AVAILABLE_BY_SPARK = "SparkSession is available by 'spark'";
  public static final String CONFIGURATION_MUST_BE_PROVIDED = "Body of  " + ENABLE_SPARK_SUPPORT + " magic command must return SparkConf object or SparkSession.Builder object";
  private final SparkEngineWithUIFactory sparkEngineWithUIFactory;

  private KernelFunctionality kernel;
  private SparkEngineNoUIFactory sparkEngineNoUIFactory;
  private SparkUIFactory sparkUIFactory;
  private SparkUiDefaults sparkUiDefaults;
  private SparkSessionBuilderFactory sparkSessionBuilderFactory;
  private SparkListenerService sparkListenerService;

  public SparkFactoryImpl(KernelFunctionality kernel,
                          SparkEngineNoUIFactory sparkEngineNoUIFactory,
                          SparkEngineWithUIFactory sparkEngineWithUIFactory,
                          SparkUIFactory sparkUIFactory,
                          SparkUiDefaults sparkUiDefaults,
                          SparkSessionBuilderFactory sparkSessionBuilderFactory,
                          SparkListenerService sparkListenerService) {
    this.kernel = kernel;
    this.sparkEngineNoUIFactory = sparkEngineNoUIFactory;
    this.sparkEngineWithUIFactory = sparkEngineWithUIFactory;
    this.sparkUIFactory = sparkUIFactory;
    this.sparkUiDefaults = sparkUiDefaults;
    this.sparkSessionBuilderFactory = sparkSessionBuilderFactory;
    this.sparkListenerService = sparkListenerService;
  }

  @Override
  public MagicCommandOutcomeItem createSpark(MagicCommandExecutionParam param, List<SparkOptionCommand> options) {
    if (noUi(options)) {
      return createSparkWithoutUI(param, options);
    } else {
      return createSparkUI(param, options);
    }
  }

  private MagicCommandOutcomeItem createSparkUI(MagicCommandExecutionParam param, List<SparkOptionCommand> options) {
    if (param.getCommandCodeBlock().isEmpty()) {
      return createSparkBasedOnEmptyConfiguration(param, options);
    } else {
      return createSparkBasedOnUserSparkConfiguration(param, (builder, message) -> {
        createAndDisplaySparkUI(options, builder, message);
        return new MagicCommandOutput(MagicCommandOutput.Status.OK, MessageCreator.get());
      });
    }
  }

  private MagicCommandOutcomeItem createSparkWithoutUI(MagicCommandExecutionParam param, List<SparkOptionCommand> options) {
    return createSparkBasedOnUserSparkConfiguration(param, (builder, message) -> {
      SparkEngineNoUI sparkEngine = sparkEngineNoUIFactory.create(builder, this.sparkSessionBuilderFactory);
      options.forEach(option -> option.run(sparkEngine, message));
      TryResult configure = sparkEngine.configure(kernel, param.getCode().getMessage());
      if (configure.isError()) {
        return new MagicCommandOutput(MagicCommandOutput.Status.ERROR, configure.error(), MessageCreator.get());
      }
      return new MagicCommandOutput(MagicCommandOutput.Status.OK, SPARK_SESSION_AVAILABLE_BY_SPARK, MessageCreator.get());
    });
  }

  private void createAndDisplaySparkUI(List<SparkOptionCommand> options, SparkSessionBuilder builder, Message message) {
    sparkUiDefaults.loadDefaults();
    SparkEngineWithUI sparkEngineWithUI = sparkEngineWithUIFactory.create(builder, this.sparkSessionBuilderFactory, this.sparkListenerService);
    options.forEach(option -> option.run(sparkEngineWithUI, message));
    SparkUIApi sparkUI = sparkUIFactory.create(builder, sparkEngineWithUI, sparkUiDefaults);
    displaySparkUI(sparkUI, message);
  }

  private MagicCommandOutcomeItem createSparkBasedOnEmptyConfiguration(MagicCommandExecutionParam param, List<SparkOptionCommand> options) {
    EvaluationObject seo = createSEO(param);
    InternalVariable.setValue(seo);
    SparkSessionBuilder config = this.sparkSessionBuilderFactory.newInstance(new SparkConf());
    createAndDisplaySparkUI(options, config, param.getCode().getMessage());
    return new MagicCommandOutput(MagicCommandOutput.Status.OK, MessageCreator.get());
  }

  private MagicCommandOutcomeItem createSparkBasedOnUserSparkConfiguration(MagicCommandExecutionParam param, SparkRunner sparkRunner) {
    EvaluationObject seo = createSEO(param);
    TryResult either = kernel.executeCode(param.getCommandCodeBlock(), seo);
    if (either.isResult()) {
      Optional<SparkSessionBuilder> builderFromUser = getBuilderFromUser(either.result());
      if (builderFromUser.isPresent()) {
        SparkSessionBuilder builder = builderFromUser.get();
        return sparkRunner.run(builder, param.getCode().getMessage());
      } else {
        return new MagicCommandOutput(MagicCommandOutput.Status.ERROR, CONFIGURATION_MUST_BE_PROVIDED, MessageCreator.get());
      }
    } else {
      return new MagicCommandOutput(MagicCommandOutput.Status.ERROR, "There occurs problem during execution of " + ENABLE_SPARK_SUPPORT + " : " + either.error(), MessageCreator.get());
    }
  }

  private Optional<SparkSessionBuilder> getBuilderFromUser(Object result) {
    if (result instanceof SparkConf) {
      return of(sparkSessionBuilderFactory.newInstance((SparkConf) result));
    } else if (result instanceof SparkSession.Builder) {
      return of(sparkSessionBuilderFactory.newInstance((SparkSession.Builder) result));
    } else {
      return Optional.empty();
    }
  }

  public interface SparkRunner {
    MagicCommandOutput run(SparkSessionBuilder builder, Message message);
  }

  private SparkUIApi displaySparkUI(SparkUIApi sparkUI, Message message) {
    Display.display(sparkUI);
    sparkUI.afterDisplay(message);
    return sparkUI;
  }

  private EvaluationObject createSEO(MagicCommandExecutionParam param) {
    return SimpleEvaluationObjectFactory.get().createSeo(param.getCommandCodeBlock(), kernel, param.getCode().getMessage(), param.getExecutionCount());
  }

  private boolean noUi(List<SparkMagicCommandOptions.SparkOptionCommand> options) {
    return options.stream()
            .anyMatch(x -> x.getName().equals(SparkOptions.NO_UI));
  }

}
