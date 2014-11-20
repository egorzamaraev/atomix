/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat;

import net.kuujo.copycat.cluster.Cluster;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.event.*;
import net.kuujo.copycat.internal.event.DefaultEvents;
import net.kuujo.copycat.internal.state.StateContext;
import net.kuujo.copycat.internal.util.Assert;
import net.kuujo.copycat.log.InMemoryLog;
import net.kuujo.copycat.log.Log;
import net.kuujo.copycat.spi.CorrelationStrategy;
import net.kuujo.copycat.spi.QuorumStrategy;
import net.kuujo.copycat.spi.TimerStrategy;
import net.kuujo.copycat.spi.protocol.Protocol;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous Copycat replica.<p>
 *
 * The Copycat replica is a fault-tolerant, replicated container for a {@link net.kuujo.copycat.StateMachine}.
 * Given a cluster of {@code AsyncCopycat} replicas, Copycat guarantees that commands and queries applied to the
 * state machine will be applied in the same order on all nodes (unless configuration specifies otherwise).<p>
 *
 * <pre>
 * {@code
 * // Create the state machine.
 * StateMachine stateMachine = new DataStore();
 *
 * // Create the log.
 * Log log = new MemoryMappedFileLog("data.log");
 *
 * // Create the cluster configuration.
 * TcpClusterConfig clusterConfig = new TcpClusterConfig()
 *   .withLocalMember(new TcpMember("localhost", 1234))
 *   .withRemoteMembers(new TcpMember("localhost", 2345), new TcpMember("localhost", 3456));
 * TcpCluster cluster = new TcpCluster(clusterConfig);
 *
 * // Create a TCP protocol.
 * NettyTcpProtocol protocol = new NettyTcpProtocol();
 *
 * // Create an asynchronous Copycat instance.
 * AsyncCopycat copycat = new AsyncCopycat(stateMachine, log, cluster, protocol);
 *
 * // Start the Copycat instance.
 * copycat.start().thenRun(() -> {
 *   copycat.submit("set", "foo", "Hello world!").thenRun(() -> {
 *     copycat.submit("get", "foo").whenComplete((result, error) -> {
 *       assertEquals("Hello world!", result);
 *     });
 *   });
 * });
 * }
 * </pre>
 *
 * In order to provide this guarantee, state machines must be designed accordingly. State machines must be
 * deterministic, meaning given the same commands in the same order, the state machine will always create
 * the same state.<p>
 *
 * To create a state machine, simple implement the {@link StateMachine} interface.
 *
 * <pre>
 * {@code
 * public class DataStore implements StateMachine {
 *   private final Map<String, Object> data = new HashMap<>();
 *
 *   @Command
 *   public void set(String name, Object value) {
 *     data.put(name, value);
 *   }
 *
 *   @Query
 *   public void get(String name) {
 *     return data.get(name);
 *   }
 *
 * }
 * }
 * </pre><p>
 *
 * Copycat will wrap this state machine on any number of nodes and ensure commands submitted
 * to the cluster are applied to the state machine in the order in which they're received.
 * Copycat supports two different types of operations - {@link net.kuujo.copycat.Command}
 * and {@link net.kuujo.copycat.Query}. {@link net.kuujo.copycat.Command} operations are write
 * operations that modify the state machine's state. All commands submitted to the cluster
 * will go through the cluster leader to ensure log order. {@link net.kuujo.copycat.Query}
 * operations are read-only operations that do not modify the state machine's state. Copycat
 * can be optionally configured to allow read-only operations on follower nodes.<p>
 *
 * As mentioned, underlying each Copycat replica is a persistent {@link net.kuujo.copycat.log.Log}.
 * The log is a strongly ordered sequence of events which Copycat replicates between leader and
 * followers. Copycat provides several {@link net.kuujo.copycat.log.Log} implementations for
 * different use cases.<p>
 *
 * Copycat also provides extensible {@link net.kuujo.copycat.spi.protocol.Protocol} support.
 * The Copycat replication implementation is completely protocol agnostic, so users can use
 * Copycat provided protocols or custom protocols. Each {@link Copycat} instance
 * is thus associated with a {@link net.kuujo.copycat.cluster.Cluster} and
 * {@link net.kuujo.copycat.spi.protocol.Protocol} which it uses for communication between replicas.
 * It is very important that all nodes within the same Copycat cluster use the same protocol for
 * obvious reasons.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class Copycat {
  protected final StateContext state;
  protected final Cluster<?> cluster;
  protected final CopycatConfig config;
  protected final Events events;

  /**
   * Constructs an asynchronous Copycat replica with a default configuration.
   *
   * @param stateMachine The Copycat state machine.
   * @param log The Copycat log.
   * @param cluster The Copycat cluster configuration.
   * @param protocol The asynchronous protocol.
   * @param <M> The cluster member type.
   */
  public <M extends Member> Copycat(StateMachine stateMachine, Log log, Cluster<M> cluster, Protocol<M> protocol) {
    this(stateMachine, log, cluster, protocol, new CopycatConfig());
  }

  /**
   * Constructs an asynchronous Copycat replica with a user-defined configuration.
   *
   * @param stateMachine The Copycat state machine.
   * @param log The Copycat log.
   * @param cluster The Copycat cluster configuration.
   * @param protocol The asynchronous protocol.
   * @param config The replica configuration.
   * @param <M> The cluster member type.
   */
  public <M extends Member> Copycat(StateMachine stateMachine, Log log, Cluster<M> cluster, Protocol<M> protocol, CopycatConfig config) {
    this(new StateContext(stateMachine, log, cluster, protocol, config), cluster, config);
  }

  private Copycat(StateContext state, Cluster<?> cluster, CopycatConfig config) {
    this.state = state;
    this.cluster = cluster;
    this.config = config;
    this.events = new DefaultEvents(state.events());
  }

  /**
   * Returns a new copycat builder.
   *
   * @return A new copycat builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the replica configuration.
   *
   * @return The replica configuration.
   */
  public CopycatConfig config() {
    return config;
  }

  /**
   * Returns the cluster configuration.
   *
   * @return The cluster configuration.
   */
  @SuppressWarnings("unchecked")
  public <M extends Member> Cluster<M> cluster() {
    return (Cluster<M>) cluster;
  }

  /**
   * Returns the context events.
   *
   * @return Context events.
   */
  public Events on() {
    return events;
  }

  /**
   * Returns the context for a specific event.
   *
   * @param event The event for which to return the context.
   * @return The event context.
   * @throws NullPointerException if {@code event} is null
   */
  public <T extends Event> EventContext<T> on(Class<T> event) {
    return events.event(Assert.isNotNull(event, "Event cannot be null"));
  }

  /**
   * Returns the event handlers registry.
   *
   * @return The event handlers registry.
   */
  public EventHandlers events() {
    return state.events();
  }

  /**
   * Returns an event handler registry for a specific event.
   *
   * @param event The event for which to return the registry.
   * @return An event handler registry.
   * @throws NullPointerException if {@code event} is null
   */
  public <T extends Event> EventHandlerRegistry<T> event(Class<T> event) {
    return state.events().event(Assert.isNotNull(event, "Event cannot be null"));
  }

  /**
   * Returns the current replica state.
   *
   * @return The current replica state.
   */
  public CopycatState state() {
    return state.state();
  }

  /**
   * Returns the current leader URI.
   *
   * @return The current leader URI.
   */
  public String leader() {
    return state.currentLeader();
  }

  /**
   * Returns a boolean indicating whether the node is the current leader.
   *
   * @return Indicates whether the node is the current leader.
   */
  public boolean isLeader() {
    return state.isLeader();
  }

  /**
   * Starts the context.
   *
   * @return A completable future to be completed once the context has started.
   */
  public CompletableFuture<Void> start() {
    return state.start();
  }

  /**
   * Stops the context.
   *
   * @return A completable future that will be completed when the context has started.
   */
  public CompletableFuture<Void> stop() {
    return state.stop();
  }

  /**
   * Submits a operation to the cluster.
   *
   * @param operation The name of the operation to submit.
   * @param args An ordered list of operation arguments.
   * @return A completable future to be completed once the result is received.
   * @throws NullPointerException if {@code operation} is null
   */
  public <R> CompletableFuture<R> submit(final String operation, final Object... args) {
    return state.submit(Assert.isNotNull(operation, "operation cannot be null"), args);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  /**
   * Asynchronous Copycat builder.
   */
  @SuppressWarnings("rawtypes")
  public static class Builder {
    CopycatConfig config = new CopycatConfig();
    Cluster cluster;
    Protocol protocol;
    StateMachine stateMachine;
    Log log = new InMemoryLog();

    private Builder() {
    }

    /**
     * Builds the copycat instance.
     *
     * @return The copycat instance.
     */
    public Copycat build() {
      return new Copycat(stateMachine, log, cluster, protocol, config);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }

    /**
     * Sets the copycat cluster.
     *
     * @param cluster The copycat cluster.
     * @return The copycat builder.
     * @throws NullPointerException if {@code cluster} is null
     */
    public Builder withCluster(Cluster<?> cluster) {
      this.cluster = Assert.isNotNull(cluster, "cluster");
      return this;
    }

    /**
     * Sets the copycat configuration.
     *
     * @param config The copycat configuration.
     * @return The copycat builder.
     * @throws NullPointerException if {@code config} is null
     */
    public Builder withConfig(CopycatConfig config) {
      this.config = Assert.isNotNull(config, "config");
      return this;
    }

    /**
     * Sets the correlation strategy.
     *
     * @param strategy The correlation strategy.
     * @return The copycat builder.
     * @throws NullPointerException if {@code strategy} is null
     */
    public Builder withCorrelationStrategy(CorrelationStrategy<?> strategy) {
      config.setCorrelationStrategy(strategy);
      return this;
    }

    /**
     * Sets the copycat election timeout.
     *
     * @param timeout The copycat election timeout.
     * @return The copycat builder.
     * @throws IllegalArgumentException if {@code timeout} is not > 0
     */
    public Builder withElectionTimeout(long timeout) {
      config.setElectionTimeout(timeout);
      return this;
    }

    /**
     * Sets the copycat heartbeat interval.
     *
     * @param interval The copycat heartbeat interval.
     * @return The copycat builder.
     * @throws IllegalArgumentException if {@code interval} is not > 0
     */
    public Builder withHeartbeatInterval(long interval) {
      config.setHeartbeatInterval(interval);
      return this;
    }

    /**
     * Sets the copycat log.
     *
     * @param log The copycat log.
     * @return The copycat builder.
     * @throws NullPointerException if {@code log} is null
     */
    public Builder withLog(Log log) {
      this.log = Assert.isNotNull(log, "log");
      return this;
    }

    /**
     * Sets the max log size.
     *
     * @param maxSize The max log size.
     * @return The copycat builder.
     * @throws IllegalArgumentException if {@code maxSize} is not > 0
     */
    public Builder withMaxLogSize(int maxSize) {
      config.setMaxLogSize(maxSize);
      return this;
    }

    /**
     * Sets the cluster protocol.
     *
     * @param protocol The cluster protocol.
     * @return The copycat builder.
     * @throws NullPointerException if {@code protocol} is null
     */
    public Builder withProtocol(Protocol<?> protocol) {
      this.protocol = Assert.isNotNull(protocol, "protocol");
      return this;
    }

    /**
     * Sets the read quorum size.
     *
     * @param quorumSize The read quorum size.
     * @return The copycat builder.
     * @throws IllegalArgumentException if {@code quorumSize} is not > -1
     */
    public Builder withReadQuorumSize(int quorumSize) {
      config.setQueryQuorumSize(quorumSize);
      return this;
    }

    /**
     * Sets the read quorum strategy.
     *
     * @param quorumStrategy The read quorum strategy.
     * @return The copycat builder.
     * @throws NullPointerException if {@code quorumStrategy} is null
     */
    public Builder withReadQuorumStrategy(QuorumStrategy quorumStrategy) {
      config.setQueryQuorumStrategy(quorumStrategy);
      return this;
    }

    /**
     * Sets whether to require quorums during reads.
     *
     * @param requireQuorum Whether to require quorums during reads.
     * @return The copycat builder.
     */
    public Builder withRequireReadQuorum(boolean requireQuorum) {
      config.setRequireQueryQuorum(requireQuorum);
      return this;
    }

    /**
     * Sets whether to require quorums during writes.
     *
     * @param requireQuorum Whether to require quorums during writes.
     * @return The copycat builder.
     */
    public Builder withRequireWriteQuorum(boolean requireQuorum) {
      config.setRequireCommandQuorum(requireQuorum);
      return this;
    }

    /**
     * Sets the copycat state machine.
     *
     * @param stateMachine The state machine.
     * @return The copycat builder.
     * @throws NullPointerException if {@code stateMachine} is null
     */
    public Builder withStateMachine(StateMachine stateMachine) {
      this.stateMachine = Assert.isNotNull(stateMachine, "stateMachine");
      return this;
    }

    /**
     * Sets the timer strategy.
     *
     * @param strategy The timer strategy.
     * @return The copycat builder.
     * @throws NullPointerException if {@code strategy} is null
     */
    public Builder withTimerStrategy(TimerStrategy strategy) {
      config.setTimerStrategy(strategy);
      return this;
    }

    /**
     * Sets the write quorum size.
     *
     * @param quorumSize The write quorum size.
     * @return The copycat builder.
     * @throws IllegalArgumentException if {@code quorumSize} is not > -1
     */
    public Builder withWriteQuorumSize(int quorumSize) {
      config.setCommandQuorumSize(quorumSize);
      return this;
    }

    /**
     * Sets the write quorum strategy.
     *
     * @param quorumStrategy The write quorum strategy.
     * @return The copycat builder.
     * @throws NullPointerException if {@code quorumStrategy} is null
     */
    public Builder withWriteQuorumStrategy(QuorumStrategy quorumStrategy) {
      config.setCommandQuorumStrategy(quorumStrategy);
      return this;
    }
  }

}