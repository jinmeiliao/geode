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
package org.apache.geode.internal.cache.tier.sockets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.EOFException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.HeapDataOutputStream;
import org.apache.geode.test.junit.categories.ClientServerTest;

@Category({ClientServerTest.class})
public class PartTest {

  @Test
  public void shouldBeMockable() throws Exception {
    Part mockPart = mock(Part.class);
    OutputStream mockOutputStream = mock(OutputStream.class);
    ByteBuffer mockByteBuffer = mock(ByteBuffer.class);

    mockPart.writeTo(mockOutputStream, mockByteBuffer);

    verify(mockPart, times(1)).writeTo(mockOutputStream, mockByteBuffer);
  }

  @Test
  public void writeToOutputStreamResetsPartOnException() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    OutputStream outputStream = mock(OutputStream.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);
    doThrow(new EOFException("test")).when(heapDataOutputStream).sendTo(eq(outputStream),
        eq(byteBuffer));

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    Throwable thrown = catchThrowable(() -> part.writeTo(outputStream, byteBuffer));

    assertThat(thrown).isInstanceOf(EOFException.class);
    verify(heapDataOutputStream, times(1)).rewind();
  }

  @Test
  public void writeToOutputStreamResetsPartOnSuccess() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    OutputStream outputStream = mock(OutputStream.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    part.writeTo(outputStream, byteBuffer);

    verify(heapDataOutputStream, times(1)).rewind();
  }

  @Test
  public void writeToByteBufferResetsPartOnException() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);
    doThrow(new BufferOverflowException()).when(heapDataOutputStream).sendTo(eq(byteBuffer));

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    Throwable thrown = catchThrowable(() -> part.writeTo(byteBuffer));

    assertThat(thrown).isInstanceOf(BufferOverflowException.class);
    verify(heapDataOutputStream, times(1)).rewind();
  }

  @Test
  public void writeToByteBufferResetsPartOnSuccess() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    part.writeTo(byteBuffer);

    verify(heapDataOutputStream, times(1)).rewind();
  }

  @Test
  public void writeToSocketChannelResetsPartOnException() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    SocketChannel socketChannel = mock(SocketChannel.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);
    doThrow(new BufferOverflowException()).when(heapDataOutputStream).sendTo(eq(socketChannel),
        eq(byteBuffer));

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    Throwable thrown = catchThrowable(() -> part.writeTo(socketChannel, byteBuffer));

    assertThat(thrown).isInstanceOf(BufferOverflowException.class);
    verify(heapDataOutputStream, times(1)).rewind();
  }

  @Test
  public void writeToSocketChannelResetsPartOnSuccess() throws Exception {
    HeapDataOutputStream heapDataOutputStream = mock(HeapDataOutputStream.class);
    when(heapDataOutputStream.size()).thenReturn(1000);
    SocketChannel socketChannel = mock(SocketChannel.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);

    Part part = new Part();
    part.setPartState(heapDataOutputStream, false);

    part.writeTo(socketChannel, byteBuffer);

    verify(heapDataOutputStream, times(1)).rewind();
  }
}
