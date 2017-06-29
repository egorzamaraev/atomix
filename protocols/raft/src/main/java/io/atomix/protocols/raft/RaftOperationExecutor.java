/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.atomix.protocols.raft;

import io.atomix.storage.buffer.HeapBytes;
import io.atomix.utils.concurrent.ThreadContext;

import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Facilitates registration and execution of state machine commands and provides deterministic scheduling.
 * <p>
 * The state machine executor is responsible for managing input to and output from a {@link RaftStateMachine}.
 * As operations are committed to the Raft log, the executor is responsible for applying them to the state machine.
 * {@link OperationType#COMMAND commands} are guaranteed to be applied to the state machine in the order in which
 * they appear in the Raft log and always in the same thread, so state machines don't have to be thread safe.
 * {@link OperationType#QUERY queries} are not generally written to the Raft log and will instead be applied according
 * to their {@link ReadConsistency}.
 * <p>
 * State machines can use the executor to provide deterministic scheduling during the execution of command callbacks.
 * <pre>
 *   {@code
 *   private Object putWithTtl(Commit<PutWithTtl> commit) {
 *     map.put(commit.operation().key(), commit);
 *     executor.schedule(Duration.ofMillis(commit.operation().ttl()), () -> {
 *       map.remove(commit.operation().key()).close();
 *     });
 *   }
 *   }
 * </pre>
 * As with all state machine callbacks, the executor will ensure scheduled callbacks are executed sequentially and
 * deterministically. As long as state machines schedule callbacks deterministically, callbacks will be executed
 * deterministically. Internally, the state machine executor triggers callbacks based on various timestamps in the
 * Raft log. This means the scheduler is dependent on internal or user-defined operations being written to the log.
 * Prior to the execution of a command, any expired scheduled callbacks will be executed based on the command's
 * logged timestamp.
 * <p>
 * It's important to note that callbacks can only be scheduled during {@link RaftOperation} operations or by recursive
 * scheduling. If a state machine attempts to schedule a callback via the executor during the execution of a
 * query, a {@link IllegalStateException} will be thrown. This is because queries are usually only applied
 * on a single state machine within the cluster, and so scheduling callbacks in reaction to query execution would
 * not be deterministic.
 *
 * @see RaftStateMachine
 * @see ServiceContext
 */
public interface RaftOperationExecutor extends ThreadContext {

  /**
   * Applies the given commit to the executor.
   *
   * @param commit the commit to apply
   * @return the commit result
   */
  byte[] apply(RaftCommit<byte[]> commit);

  /**
   * Registers a operation callback.
   *
   * @param operationId the operation identifier
   * @param callback the operation callback
   * @throws NullPointerException if the {@code operationId} or {@code callback} is null
   */
  void handle(OperationId operationId, Function<RaftCommit<byte[]>, byte[]> callback);

  /**
   * Registers a operation callback.
   *
   * @param operationId the operation identifier
   * @param callback the operation callback
   * @throws NullPointerException if the {@code operationId} or {@code callback} is null
   */
  default void register(OperationId operationId, Consumer<RaftCommit<Void>> callback) {
    checkNotNull(operationId, "operationId cannot be null");
    checkNotNull(callback, "callback cannot be null");
    handle(operationId, commit -> {
      callback.accept(commit.mapToNull());
      return HeapBytes.EMPTY;
    });
  }

  /**
   * Registers a operation callback.
   *
   * @param operationId the operation identifier
   * @param callback the operation callback
   * @throws NullPointerException if the {@code operationId} or {@code callback} is null
   */
  default <R> void register(OperationId operationId, Function<RaftCommit<Void>, R> callback, Function<R, byte[]> encoder) {
    checkNotNull(operationId, "operationId cannot be null");
    checkNotNull(callback, "callback cannot be null");
    checkNotNull(encoder, "encoder cannot be null");
    handle(operationId, commit -> encoder.apply(callback.apply(commit.mapToNull())));
  }

  /**
   * Registers a operation callback.
   *
   * @param operationId the operation identifier
   * @param decoder the operation decoder
   * @param callback the operation callback
   * @throws NullPointerException if the {@code operationId} or {@code callback} is null
   */
  default <T> void register(OperationId operationId, Function<byte[], T> decoder, Consumer<RaftCommit<T>> callback) {
    checkNotNull(operationId, "operationId cannot be null");
    checkNotNull(decoder, "decoder cannot be null");
    checkNotNull(callback, "callback cannot be null");
    handle(operationId, commit -> {
      callback.accept(commit.map(decoder));
      return HeapBytes.EMPTY;
    });
  }

  /**
   * Registers an operation callback.
   *
   * @param operationId the operation identifier
   * @param decoder the operation decoder
   * @param callback the operation callback
   * @param encoder the output encoder
   * @throws NullPointerException if the {@code operationId} or {@code callback} is null
   */
  default <T, R> void register(OperationId operationId, Function<byte[], T> decoder, Function<RaftCommit<T>, R> callback, Function<R, byte[]> encoder) {
    checkNotNull(operationId, "operationId cannot be null");
    checkNotNull(decoder, "decoder cannot be null");
    checkNotNull(callback, "callback cannot be null");
    checkNotNull(encoder, "encoder cannot be null");
    handle(operationId, commit -> encoder.apply(callback.apply(commit.map(decoder))));
  }

  @Override
  default void close() {
  }
}
