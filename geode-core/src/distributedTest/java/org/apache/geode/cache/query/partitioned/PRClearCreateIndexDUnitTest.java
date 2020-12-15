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

package org.apache.geode.cache.query.partitioned;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.distributed.internal.DistributionMessageObserver;
import org.apache.geode.internal.cache.DistributedClearOperation;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;

public class PRClearCreateIndexDUnitTest implements Serializable {
  @Rule
  public ClusterStartupRule cluster = new ClusterStartupRule(3, true);

  private MemberVM primary, secondary;

  @Before
  public void before() throws Exception {
    int locatorPort = ClusterStartupRule.getDUnitLocatorPort();
    primary = cluster.startServerVM(0, locatorPort);
    secondary = cluster.startServerVM(1, locatorPort);

    // set up message observer in the secondary member to watch for message received
    secondary.invoke(() -> {
      DistributionMessageObserver.setInstance(new MessageObserver());
    });
  }

  @After
  public void after() throws Exception {
    secondary.invoke(() -> {
      DistributionMessageObserver.setInstance(null);
    });
  }

  @Test
  public void createIndexOnSecondaryMemberWillBlockClear() throws Exception {
    // create region on server1 first, making sure server1 has the primary bucket
    primary.invoke(() -> {
      Region<Object, Object> region =
          ClusterStartupRule.memberStarter.createPartitionRegion("regionA", f -> {
          },
              f -> f.setTotalNumBuckets(1).setRedundantCopies(1));
      IntStream.range(0, 100).forEach(i -> region.put(i, "value" + i));
    });

    // server2 has the secondary bucket
    secondary.invoke(() -> {
      ClusterStartupRule.memberStarter.createPartitionRegion("regionA", f -> {
      },
          f -> f.setTotalNumBuckets(1).setRedundantCopies(1));
    });

    // no matter where clear is called, a "PartitionedRegionClearMessage" is always sent to the
    // primary member to start (unless it's called from the primary member),
    // thus request a lock. So if we create index on the primary, the clear and createIndex will run
    // sequentially. But if we create index on the secondary member, the secondary member will not
    // request a lock for clear operation, thus resulting in an EntryDestroyedException when create
    // index is happening.

    // for replicate region, ClearRegionMessage.operationOnRegion OP_LOCK_FOR_CLEAR message is sent
    // to the secondary members first, we should do the same for partition regions
    AsyncInvocation createIndex = secondary.invokeAsync(() -> {
      QueryService queryService = ClusterStartupRule.getCache().getQueryService();
      // run create index multiple times to make sure the clear operation fall inside a
      // createIndex Operation
      IntStream.range(0, 10).forEach(i -> {
        try {
          queryService.createIndex("index" + i, "name" + i, "/regionA");
        } catch (Exception e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      });
    });

    AsyncInvocation clear = primary.invokeAsync(() -> {
      // start the clear a bit later that the createIndex operation
      Thread.sleep(200);
      Region region = ClusterStartupRule.getCache().getRegion("/regionA");
      region.clear();
    });

    createIndex.get();
    clear.get();

    // assert that secondary member received these messages
    secondary.invoke(() -> {
      MessageObserver observer = (MessageObserver) DistributionMessageObserver.getInstance();
      assertThat(observer.isClearReceived()).isTrue();
      assertThat(observer.isLockReceived()).isTrue();
    });
  }

  private static class MessageObserver extends DistributionMessageObserver {
    private boolean lockReceived = false;
    private boolean clearReceived = false;

    @Override
    public void beforeProcessMessage(ClusterDistributionManager dm, DistributionMessage message) {
      if (message instanceof DistributedClearOperation.ClearRegionMessage) {
        DistributedClearOperation.ClearRegionMessage clearMessage =
            (DistributedClearOperation.ClearRegionMessage) message;
        if (clearMessage
            .getOperationType() == DistributedClearOperation.OperationType.OP_LOCK_FOR_CLEAR) {
          lockReceived = true;
        }
        if (clearMessage.getOperationType() == DistributedClearOperation.OperationType.OP_CLEAR) {
          clearReceived = true;
        }
      }
    }

    public boolean isLockReceived() {
      return lockReceived;
    }

    public boolean isClearReceived() {
      return clearReceived;
    }
  }

}
