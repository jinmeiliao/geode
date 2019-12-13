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

package org.apache.geode.management.configuration;

import java.util.List;
import java.util.stream.Collectors;

public class SuperRegion extends Region {
  private List<Region> regionPerGroup;
  private List<String> groups;

  public SuperRegion(){}

  public SuperRegion(List<Region> regionPerGroup) {
    if (regionPerGroup.size() < 2) {
      throw new IllegalArgumentException("needs to have multiple regions.");
    }
    this.regionPerGroup = regionPerGroup;
    groups = regionPerGroup.stream().map(Region::getGroup).collect(
        Collectors.toList());
    Region region = regionPerGroup.get(0);
    setName(region.getName());
  }

  public List<Region> getRegionPerGroup() {
    return regionPerGroup;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setRegionPerGroup(List<Region> regionPerGroup) {
    this.regionPerGroup = regionPerGroup;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }
}
