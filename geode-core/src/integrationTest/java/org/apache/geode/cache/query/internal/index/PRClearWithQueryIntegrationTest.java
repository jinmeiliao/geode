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

package org.apache.geode.cache.query.internal.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.management.model.Item;
import org.apache.geode.test.junit.rules.ExecutorServiceRule;
import org.apache.geode.test.junit.rules.ServerStarterRule;

public class PRClearWithQueryIntegrationTest {
  @Rule
  public ServerStarterRule server = new ServerStarterRule()
      .withServerCount(0).withAutoStart()
      .withRegion(RegionShortcut.PARTITION, "regionA");

  @Rule
  public ExecutorServiceRule executor = new ExecutorServiceRule();

  @Test
  public void test() throws Exception {
    Region region = server.getCache().getRegion("/regionA");
    IntStream.range(0, 10).forEach(i -> {
      String id = "Item" + i;
      region.put(id, new Item(id));
    });
    QueryService queryService = server.getCache().getQueryService();
    queryService.createKeyIndex("primaryKeyIndex", "r.id", "/regionA r");

    CompletableFuture<Void> queryTask = executor.runAsync(() -> {
      Query query = queryService.newQuery("select * from /regionA r where r.id='Item9'");
      try {
        SelectResults selectResult = (SelectResults) query.execute();
        assertThat(selectResult).hasSize(1);
      } catch (Exception e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    });

//    CompletableFuture<Void> otherTask = executor.runAsync(() -> {
//      try {
//        PrimaryKeyIndex.POINT_LATCH.await();
//      } catch (InterruptedException e) {}
//      Object item = region.remove("Item9");
//      PrimaryKeyIndex.CLEAR_DONE.countDown();
//      assertThat(item).isNull();
//    });

    CompletableFuture<Void> clearTask = executor.runAsync(() -> {
      try {
        PrimaryKeyIndex.POINT_LATCH.await();
      } catch (InterruptedException e) {
      }
      region.clear();
      PrimaryKeyIndex.CLEAR_DONE.countDown();
    });

    queryTask.get();
    clearTask.get();
//    otherTask.get();
  }
}
