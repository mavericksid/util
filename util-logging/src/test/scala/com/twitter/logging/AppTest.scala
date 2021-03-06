/*
 * Copyright 2010 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.logging

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import com.twitter.app.App

@RunWith(classOf[JUnitRunner])
class AppTest extends FunSuite {

  object TestLoggingApp extends App with Logging {
    override def handlers = ScribeHandler() :: super.handlers
  }

  test("TestLoggingApp should have one factory with two log handlers") {
    assert(TestLoggingApp.loggerFactories.size === 1)
    assert(TestLoggingApp.loggerFactories.head.handlers.size === 2)
  }
}
