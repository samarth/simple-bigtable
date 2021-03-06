/*-
 * -\-\-
 * simple-bigtable
 * --
 * Copyright (C) 2016 - 2017 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

/*
 *
 *  * Copyright 2016 Spotify AB.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package com.spotify.bigtable.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.util.Lists;
import com.google.bigtable.v2.Family;
import com.google.bigtable.v2.ReadRowsRequest;
import com.google.bigtable.v2.Row;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.spotify.bigtable.BigtableMock;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class FamiliesReadImplTest {
  BigtableMock bigtableMock = BigtableMock.getMock();
  ReadFamilies.FamiliesWithinRowRead.ReadImpl familiesRead;

  @Before
  public void setUp() throws Exception {
    final TableRead.TableReadImpl tableRead = new TableRead.TableReadImpl(bigtableMock, "table");
    familiesRead = new ReadFamilies.FamiliesWithinRowRead.ReadImpl(tableRead.row("row"));
  }

  private void verifyReadRequest(ReadRowsRequest.Builder readRequest) throws Exception {
    assertEquals(bigtableMock.getFullTableName("table"), readRequest.getTableName());
    assertEquals("row", readRequest.getRows().getRowKeys(0).toStringUtf8());
    assertEquals(1, readRequest.getRows().getRowKeysCount());
    assertEquals(0, readRequest.getRows().getRowRangesCount());
    assertEquals(1, readRequest.getRowsLimit());
  }

  @Test
  public void testGetClient() throws Exception {
    assertEquals(bigtableMock.getMockedDataClient(), familiesRead.getClient());
  }

  @Test
  public void testParentDataTypeToDataType() throws Exception {
    assertEquals(Lists.newArrayList(), familiesRead.parentTypeToCurrentType().apply(Optional.empty()));
    assertEquals(Lists.newArrayList(), familiesRead.parentTypeToCurrentType().apply(Optional.of(Row.getDefaultInstance())));

    final Family family = Family.getDefaultInstance();
    final Row row = Row.newBuilder().addFamilies(family).build();
    assertEquals(ImmutableList.of(family), familiesRead.parentTypeToCurrentType().apply(Optional.of(row)));
  }

  @Test
  public void testExecuteAsync() throws Exception {
    verifyReadRequest(familiesRead.readRequest());
    when(bigtableMock.getMockedDataClient().readRowsAsync(any()))
            .thenReturn(Futures.immediateFuture(Collections.emptyList()));

    familiesRead.executeAsync();

    verifyReadRequest(familiesRead.readRequest()); // make sure execute did not change the read request
    verify(bigtableMock.getMockedDataClient()).readRowsAsync(familiesRead.readRequest().build());
    verifyNoMoreInteractions(bigtableMock.getMockedDataClient());
  }
}
