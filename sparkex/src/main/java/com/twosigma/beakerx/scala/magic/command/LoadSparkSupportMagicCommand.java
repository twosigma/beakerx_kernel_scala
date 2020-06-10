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

import com.twosigma.beakerx.TryResult;
import com.twosigma.beakerx.evaluator.SimpleEvaluationObjectFactory;
import com.twosigma.beakerx.jvm.object.EvaluationObject;
import com.twosigma.beakerx.kernel.ImportPath;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.magic.command.CodeFactory;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandExecutionParam;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandFunctionality;
import com.twosigma.beakerx.kernel.magic.command.functionality.LoadMagicMagicCommand;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutcomeItem;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutput;
import com.twosigma.beakerx.kernel.msg.JupyterMessages;
import com.twosigma.beakerx.kernel.msg.MessageCreator;
import com.twosigma.beakerx.message.Header;
import com.twosigma.beakerx.message.Message;
import com.twosigma.beakerx.scala.spark.SparkDisplayers;
import com.twosigma.beakerx.scala.spark.SparkImplicit;
import com.twosigma.beakerx.scala.spark.TimeSeriesRDDDisplayer;

import java.util.Optional;

public class LoadSparkSupportMagicCommand implements MagicCommandFunctionality {

  public static final String LOAD_SPARK_SUPPORT = "%loadSparkSupport";
  private KernelFunctionality kernel;

  public LoadSparkSupportMagicCommand(KernelFunctionality kernel) {
    this.kernel = kernel;
  }

  @Override
  public String getMagicCommandName() {
    return LOAD_SPARK_SUPPORT;
  }

  @Override
  public MagicCommandOutcomeItem execute(MagicCommandExecutionParam param) {
    TryResult implicits = addImplicits(param.getCode().getMessage(), new SparkImplicit().codeAsString());
    if (implicits.isError()) {
      return new MagicCommandOutput(MagicCommandOutput.Status.ERROR, implicits.error(),MessageCreator.get());
    }
    MagicCommandOutcomeItem loadSpark = loadSparkMagic();
    if (!loadSpark.getStatus().equals(MagicCommandOutcomeItem.Status.OK)) {
      return new MagicCommandOutput(MagicCommandOutput.Status.ERROR, "Can not run spark support",MessageCreator.get());
    }
    loadTwosigmaFlintSupport(param.getCode().getMessage());
    SparkDisplayers.register();
    addDefaultImports();
    return new MagicCommandOutput(MagicCommandOutput.Status.OK, "Spark support enabled", MessageCreator.get());
  }

  private void loadTwosigmaFlintSupport(Message parent) {
    if (kernel.checkIfClassExistsInClassloader("com.twosigma.flint.timeseries.TimeSeriesRDD")) {
      TimeSeriesRDDDisplayer.register();
      addImplicits(parent, TimeSeriesRDDDisplayer.implicitCodeAsString());
    }
  }

  private MagicCommandOutcomeItem loadSparkMagic() {
    CodeFactory codeFactory = new CodeFactory(MessageCreator.get(), new SimpleEvaluationObjectFactory());
    Optional<MagicCommandFunctionality> magic = codeFactory.findMagicCommandFunctionality(kernel.getMagicCommandTypes(), LoadMagicMagicCommand.LOAD_MAGIC);
    MagicCommandOutcomeItem magicCommandOutcomeItem = ((LoadMagicMagicCommand) magic.get())
            .load(SparkMagicCommand.class.getName());
    return magicCommandOutcomeItem;
  }

  private TryResult addImplicits(Message parent, String codeToExecute) {
    EvaluationObject seo = SimpleEvaluationObjectFactory.get().createSeo(
            codeToExecute,
            kernel,
            new Message(new Header(JupyterMessages.COMM_MSG, parent.getHeader().getSession())),
            1);
    return kernel.executeCode(codeToExecute, seo);
  }

  private void addDefaultImports() {
    kernel.addImport(new ImportPath("org.apache.spark.sql.SparkSession"));
  }

}
