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
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutcomeItem;
import com.twosigma.beakerx.kernel.msg.MessageCreator;

import java.util.Optional;

class EnableSparkSupportMagicSparkConfiguration implements EnableSparkSupportMagicCommand.EnableSparkSupportMagicConfiguration {

  private KernelFunctionality kernel;

  public EnableSparkSupportMagicSparkConfiguration(KernelFunctionality kernel) {
    this.kernel = kernel;
  }

  @Override
  public MagicCommandOutcomeItem run(MagicCommandExecutionParam param) {
    String loadSparkMagic = "%%sparkRunner";
    Optional<MagicCommandFunctionality> magic = new CodeFactory(MessageCreator.get(), new SimpleEvaluationObjectFactory()).findMagicCommandFunctionality(kernel.getMagicCommandTypes(), loadSparkMagic);
    return magic.get().execute(param);
  }

  @Override
  public boolean isInit() {
    return false;
  }
}
