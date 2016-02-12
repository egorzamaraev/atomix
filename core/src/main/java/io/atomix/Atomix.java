/*
 * Copyright 2016 the original author or authors.
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
 * limitations under the License
 */
package io.atomix;

import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;
import io.atomix.catalyst.util.concurrent.ThreadContext;
import io.atomix.collections.DistributedMap;
import io.atomix.collections.DistributedMultiMap;
import io.atomix.collections.DistributedQueue;
import io.atomix.collections.DistributedSet;
import io.atomix.coordination.DistributedGroup;
import io.atomix.coordination.DistributedLock;
import io.atomix.manager.ResourceClient;
import io.atomix.manager.ResourceManager;
import io.atomix.messaging.DistributedMessageBus;
import io.atomix.messaging.DistributedTaskQueue;
import io.atomix.messaging.DistributedTopic;
import io.atomix.resource.Resource;
import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;
import io.atomix.variables.DistributedValue;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Base type for creating and managing distributed {@link Resource resources} in a Atomix cluster.
 * <p>
 * Resources are user provided stateful objects backed by a distributed state machine. This class facilitates the
 * creation and management of {@link Resource} objects via a filesystem like interface. There is a
 * one-to-one relationship between keys and resources, so each key can be associated with one and only one resource.
 * <p>
 * To create a resource, pass the resource {@link java.lang.Class} to the {@link Atomix#get(String, ResourceType)} method.
 * When a resource is created, the {@link io.atomix.copycat.server.StateMachine} associated with the resource will be created on each Raft server
 * and future operations submitted for that resource will be applied to the state machine. Internally, resource state
 * machines are multiplexed across a shared Raft log.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public abstract class Atomix implements ResourceManager<Atomix> {
  final ResourceClient client;

  protected Atomix(ResourceClient client) {
    this.client = Assert.notNull(client, "client");
  }

  @Override
  public ThreadContext context() {
    return client.context();
  }

  /**
   * Returns the Atomix serializer.
   * <p>
   * Serializable types registered on the returned serializer are reflected throughout the system. Types
   * registered on a client must also be registered on a server to be transported across the wire.
   *
   * @return The Atomix serializer.
   */
  public Serializer serializer() {
    return client.client().serializer();
  }

  /**
   * Gets or creates a distributed map.
   *
   * @param key The resource key.
   * @param <K> The map key type.
   * @param <V> The map value type.
   * @return A completable future to be completed once the map has been created.
   */
  public <K, V> CompletableFuture<DistributedMap<K, V>> getMap(String key) {
    return get(key, DistributedMap.class);
  }

  /**
   * Gets or creates a distributed map.
   *
   * @param key The resource key.
   * @param options The map options.
   * @param <K> The map key type.
   * @param <V> The map value type.
   * @return A completable future to be completed once the map has been created.
   */
  public <K, V> CompletableFuture<DistributedMap<K, V>> getMap(String key, DistributedMap.Options options) {
    return get(key, DistributedMap.class, options);
  }

  /**
   * Gets or creates a distributed map.
   *
   * @param key The resource key.
   * @param config The map configuration.
   * @param <K> The map key type.
   * @param <V> The map value type.
   * @return A completable future to be completed once the map has been created.
   */
  public <K, V> CompletableFuture<DistributedMap<K, V>> getMap(String key, DistributedMap.Config config) {
    return get(key, DistributedMap.class, config);
  }

  /**
   * Gets or creates a distributed map.
   *
   * @param key The resource key.
   * @param config The map configuration.
   * @param options The map options.
   * @param <K> The map key type.
   * @param <V> The map value type.
   * @return A completable future to be completed once the map has been created.
   */
  public <K, V> CompletableFuture<DistributedMap<K, V>> getMap(String key, DistributedMap.Config config, DistributedMap.Options options) {
    return get(key, DistributedMap.class, config, options);
  }

  /**
   * Gets or creates a distributed multi map.
   *
   * @param key The resource key.
   * @param <K> The multi map key type.
   * @param <V> The multi map value type.
   * @return A completable future to be completed once the multi map has been created.
   */
  public <K, V> CompletableFuture<DistributedMultiMap<K, V>> getMultiMap(String key) {
    return get(key, DistributedMultiMap.class);
  }

  /**
   * Gets or creates a distributed multi map.
   *
   * @param key The resource key.
   * @param <K> The multi map key type.
   * @param <V> The multi map value type.
   * @return A completable future to be completed once the multi map has been created.
   */
  public <K, V> CompletableFuture<DistributedMultiMap<K, V>> getMultiMap(String key, DistributedMultiMap.Config config) {
    return get(key, DistributedMultiMap.class, config);
  }

  /**
   * Gets or creates a distributed multi map.
   *
   * @param key The resource key.
   * @param <K> The multi map key type.
   * @param <V> The multi map value type.
   * @return A completable future to be completed once the multi map has been created.
   */
  public <K, V> CompletableFuture<DistributedMultiMap<K, V>> getMultiMap(String key, DistributedMultiMap.Options options) {
    return get(key, DistributedMultiMap.class, options);
  }

  /**
   * Gets or creates a distributed multi map.
   *
   * @param key The resource key.
   * @param <K> The multi map key type.
   * @param <V> The multi map value type.
   * @return A completable future to be completed once the multi map has been created.
   */
  public <K, V> CompletableFuture<DistributedMultiMap<K, V>> getMultiMap(String key, DistributedMultiMap.Config config, DistributedMultiMap.Options options) {
    return get(key, DistributedMultiMap.class, config, options);
  }

  /**
   * Gets or creates a distributed set.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the set has been created.
   */
  public <T> CompletableFuture<DistributedSet<T>> getSet(String key) {
    return get(key, DistributedSet.class);
  }

  /**
   * Gets or creates a distributed set.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the set has been created.
   */
  public <T> CompletableFuture<DistributedSet<T>> getSet(String key, DistributedSet.Config config) {
    return get(key, DistributedSet.class, config);
  }

  /**
   * Gets or creates a distributed set.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the set has been created.
   */
  public <T> CompletableFuture<DistributedSet<T>> getSet(String key, DistributedSet.Options options) {
    return get(key, DistributedSet.class, options);
  }

  /**
   * Gets or creates a distributed set.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the set has been created.
   */
  public <T> CompletableFuture<DistributedSet<T>> getSet(String key, DistributedSet.Config config, DistributedSet.Options options) {
    return get(key, DistributedSet.class, config, options);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedQueue<T>> getQueue(String key) {
    return get(key, DistributedQueue.class);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedQueue<T>> getQueue(String key, DistributedQueue.Config config) {
    return get(key, DistributedQueue.class, config);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedQueue<T>> getQueue(String key, DistributedQueue.Options options) {
    return get(key, DistributedQueue.class, options);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedQueue<T>> getQueue(String key, DistributedQueue.Config config, DistributedQueue.Options options) {
    return get(key, DistributedQueue.class, config, options);
  }

  /**
   * Gets or creates a distributed value.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the value has been created.
   */
  public <T> CompletableFuture<DistributedValue<T>> getValue(String key) {
    return get(key, DistributedValue.class);
  }

  /**
   * Gets or creates a distributed value.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the value has been created.
   */
  public <T> CompletableFuture<DistributedValue<T>> getValue(String key, DistributedValue.Config config) {
    return get(key, DistributedValue.class, config);
  }

  /**
   * Gets or creates a distributed value.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the value has been created.
   */
  public <T> CompletableFuture<DistributedValue<T>> getValue(String key, DistributedValue.Options options) {
    return get(key, DistributedValue.class, options);
  }

  /**
   * Gets or creates a distributed value.
   *
   * @param key The resource key.
   * @param <T> The value type.
   * @return A completable future to be completed once the value has been created.
   */
  public <T> CompletableFuture<DistributedValue<T>> getValue(String key, DistributedValue.Config config, DistributedValue.Options options) {
    return get(key, DistributedValue.class, config, options);
  }

  /**
   * Gets or creates a distributed long.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the long has been created.
   */
  public CompletableFuture<DistributedLong> getLong(String key) {
    return get(key, DistributedLong.class);
  }

  /**
   * Gets or creates a distributed long.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the long has been created.
   */
  public CompletableFuture<DistributedLong> getLong(String key, DistributedLong.Config config) {
    return get(key, DistributedLong.class, config);
  }

  /**
   * Gets or creates a distributed long.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the long has been created.
   */
  public CompletableFuture<DistributedLong> getLong(String key, DistributedLong.Options options) {
    return get(key, DistributedLong.class, options);
  }

  /**
   * Gets or creates a distributed long.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the long has been created.
   */
  public CompletableFuture<DistributedLong> getLong(String key, DistributedLong.Config config, DistributedLong.Options options) {
    return get(key, DistributedLong.class, config, options);
  }

  /**
   * Gets or creates a distributed lock.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the lock has been created.
   */
  public CompletableFuture<DistributedLock> getLock(String key) {
    return get(key, DistributedLock.class);
  }

  /**
   * Gets or creates a distributed lock.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the lock has been created.
   */
  public CompletableFuture<DistributedLock> getLock(String key, DistributedLock.Config config) {
    return get(key, DistributedLock.class, config);
  }

  /**
   * Gets or creates a distributed lock.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the lock has been created.
   */
  public CompletableFuture<DistributedLock> getLock(String key, DistributedLock.Options options) {
    return get(key, DistributedLock.class, options);
  }

  /**
   * Gets or creates a distributed lock.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the lock has been created.
   */
  public CompletableFuture<DistributedLock> getLock(String key, DistributedLock.Config config, DistributedLock.Options options) {
    return get(key, DistributedLock.class, config, options);
  }

  /**
   * Gets or creates a distributed group.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the group has been created.
   */
  public CompletableFuture<DistributedGroup> getGroup(String key) {
    return get(key, DistributedGroup.class);
  }

  /**
   * Gets or creates a distributed group.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the group has been created.
   */
  public CompletableFuture<DistributedGroup> getGroup(String key, DistributedGroup.Config config) {
    return get(key, DistributedGroup.class, config);
  }

  /**
   * Gets or creates a distributed group.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the group has been created.
   */
  public CompletableFuture<DistributedGroup> getGroup(String key, DistributedGroup.Options options) {
    return get(key, DistributedGroup.class, options);
  }

  /**
   * Gets or creates a distributed group.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the group has been created.
   */
  public CompletableFuture<DistributedGroup> getGroup(String key, DistributedGroup.Config config, DistributedGroup.Options options) {
    return get(key, DistributedGroup.class, config, options);
  }

  /**
   * Gets or creates a distributed topic.
   *
   * @param key The resource key.
   * @param <T> The topic message type.
   * @return A completable future to be completed once the topic has been created.
   */
  public <T> CompletableFuture<DistributedTopic<T>> getTopic(String key) {
    return get(key, DistributedTopic.class);
  }

  /**
   * Gets or creates a distributed topic.
   *
   * @param key The resource key.
   * @param <T> The topic message type.
   * @return A completable future to be completed once the topic has been created.
   */
  public <T> CompletableFuture<DistributedTopic<T>> getTopic(String key, DistributedTopic.Config config) {
    return get(key, DistributedTopic.class, config);
  }

  /**
   * Gets or creates a distributed topic.
   *
   * @param key The resource key.
   * @param <T> The topic message type.
   * @return A completable future to be completed once the topic has been created.
   */
  public <T> CompletableFuture<DistributedTopic<T>> getTopic(String key, DistributedTopic.Options options) {
    return get(key, DistributedTopic.class, options);
  }

  /**
   * Gets or creates a distributed topic.
   *
   * @param key The resource key.
   * @param <T> The topic message type.
   * @return A completable future to be completed once the topic has been created.
   */
  public <T> CompletableFuture<DistributedTopic<T>> getTopic(String key, DistributedTopic.Config config, DistributedTopic.Options options) {
    return get(key, DistributedTopic.class, config, options);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The queue message type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedTaskQueue<T>> getTaskQueue(String key) {
    return get(key, DistributedTaskQueue.class);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The queue message type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedTaskQueue<T>> getTaskQueue(String key, DistributedTaskQueue.Config config) {
    return get(key, DistributedTaskQueue.class, config);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The queue message type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedTaskQueue<T>> getTaskQueue(String key, DistributedTaskQueue.Options options) {
    return get(key, DistributedTaskQueue.class, options);
  }

  /**
   * Gets or creates a distributed queue.
   *
   * @param key The resource key.
   * @param <T> The queue message type.
   * @return A completable future to be completed once the queue has been created.
   */
  public <T> CompletableFuture<DistributedTaskQueue<T>> getTaskQueue(String key, DistributedTaskQueue.Config config, DistributedTaskQueue.Options options) {
    return get(key, DistributedTaskQueue.class, config, options);
  }

  /**
   * Gets or creates a distributed message bus.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the message bus has been created.
   */
  public CompletableFuture<DistributedMessageBus> getMessageBus(String key) {
    return get(key, DistributedMessageBus.class);
  }

  /**
   * Gets or creates a distributed message bus.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the message bus has been created.
   */
  public CompletableFuture<DistributedMessageBus> getMessageBus(String key, DistributedMessageBus.Config config) {
    return get(key, DistributedMessageBus.class, config);
  }

  /**
   * Gets or creates a distributed message bus.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the message bus has been created.
   */
  public CompletableFuture<DistributedMessageBus> getMessageBus(String key, DistributedMessageBus.Options options) {
    return get(key, DistributedMessageBus.class, options);
  }

  /**
   * Gets or creates a distributed message bus.
   *
   * @param key The resource key.
   * @return A completable future to be completed once the message bus has been created.
   */
  public CompletableFuture<DistributedMessageBus> getMessageBus(String key, DistributedMessageBus.Config config, DistributedMessageBus.Options options) {
    return get(key, DistributedMessageBus.class, config, options);
  }

  @Override
  public ResourceType type(Class<? extends Resource<?>> type) {
    return client.type(type);
  }

  @Override
  public CompletableFuture<Boolean> exists(String key) {
    return client.exists(key);
  }

  @Override
  public CompletableFuture<Set<String>> keys() {
    return client.keys().thenApply(this::cleanKeys);
  }

  @Override
  public <T extends Resource> CompletableFuture<Set<String>> keys(Class<? super T> type) {
    return client.keys(type).thenApply(this::cleanKeys);
  }

  @Override
  public CompletableFuture<Set<String>> keys(ResourceType type) {
    return client.keys(type).thenApply(this::cleanKeys);
  }

  /**
   * Cleans the key set.
   */
  private Set<String> cleanKeys(Set<String> keys) {
    keys.remove("");
    return keys;
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, Class<? super T> type) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, null, null);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, Class<? super T> type, Resource.Config config) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, config, null);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, Class<? super T> type, Resource.Options options) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, null, options);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, Class<? super T> type, Resource.Config config, Resource.Options options) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, config, options);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, ResourceType type) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, null, null);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, ResourceType type, Resource.Config config) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, config, null);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, ResourceType type, Resource.Options options) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, null, options);
  }

  @Override
  public <T extends Resource> CompletableFuture<T> get(String key, ResourceType type, Resource.Config config, Resource.Options options) {
    Assert.argNot(key.trim().length() == 0, "invalid resource key: key must be of non-zero length");
    return client.get(key, type, config, options);
  }

  @Override
  public CompletableFuture<Atomix> open() {
    return client.open().thenApply(v -> this);
  }

  @Override
  public boolean isOpen() {
    return client.isOpen();
  }

  @Override
  public CompletableFuture<Void> close() {
    return client.close();
  }

  @Override
  public boolean isClosed() {
    return client.isClosed();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
