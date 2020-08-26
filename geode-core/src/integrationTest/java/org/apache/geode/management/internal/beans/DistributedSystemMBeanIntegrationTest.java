/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.management.internal.beans;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.management.DistributedSystemMXBean;
import org.apache.geode.management.internal.cli.json.QueryResultFormatter;
import org.apache.geode.management.model.Employee;
import org.apache.geode.test.junit.assertions.TabularResultModelAssert;
import org.apache.geode.test.junit.rules.GfshCommandRule;
import org.apache.geode.test.junit.rules.MBeanServerConnectionRule;
import org.apache.geode.test.junit.rules.ServerStarterRule;

public class DistributedSystemMBeanIntegrationTest {

  public static final String SELECT_ALL = "select * from /testRegion r where r.id=1";

  @ClassRule
  public static ServerStarterRule server = new ServerStarterRule()
      .withNoCacheServer()
      .withJMXManager()
      .withRegion(RegionShortcut.REPLICATE, "testRegion")
      .withAutoStart();

  @Rule
  public MBeanServerConnectionRule connectionRule =
      new MBeanServerConnectionRule(server::getJmxPort);

  @Rule
  public GfshCommandRule gfsh = new GfshCommandRule();

  private DistributedSystemMXBean bean;

  private static Date date;
  private static java.sql.Date sqlDate;
  private static LocalDate localDate;

  @BeforeClass
  public static void setupClass() throws Exception {
    Region<Object, Object> testRegion = server.getCache().getRegion("testRegion");
    localDate = LocalDate.of(2020, 1, 1);
    sqlDate = java.sql.Date.valueOf(localDate);
    date = new Date(sqlDate.getTime());
    Employee employee = new Employee();
    employee.setId(1);
    employee.setName("John");
    employee.setTitle("Manager");
    employee.setStartDate(date);
    employee.setEndDate(sqlDate);
    employee.setBirthday(localDate);
    testRegion.put(1, employee);
  }

  @Before
  public void setup() throws Exception {
    connectionRule.connect(server.getJmxPort());
    bean = connectionRule.getProxyMXBean(DistributedSystemMXBean.class);
  }

  // this is to make sure dates are formatted correctly
  @Test
  public void queryUsingMBeanFormatsDateCorrectly() throws Exception {
    SimpleDateFormat formater =
        new SimpleDateFormat(QueryResultFormatter.DATE_FORMAT_PATTERN);
    String dateString = formater.format(date);
    String result = bean.queryData(SELECT_ALL, "server", 100);
    System.out.println(result);
    assertThat(result)
        .contains("\"java.util.Date\",\"" + dateString + "\"")
        .contains("\"java.sql.Date\",\"" + dateString + "\"");
  }

  // this is simply to document the current behavior of gfsh
  // gfsh doesn't attempt tp format the date objects as of now
  @Test
  public void queryUsingGfshDoesNotFormatDate() throws Exception {
    gfsh.connectAndVerify(server.getJmxPort(), GfshCommandRule.PortType.jmxManager);
    TabularResultModelAssert tabularAssert =
        gfsh.executeAndAssertThat("query --query='" + SELECT_ALL + "'")
            .statusIsSuccess()
            .hasTableSection();
    tabularAssert.hasColumn("startDate").containsExactly(date.getTime() + "");
    tabularAssert.hasColumn("endDate").containsExactly(sqlDate.getTime() + "");
  }
}
