/*
 * Copyright 2015 the original author or authors.
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
package net.kuujo.copycat.io.serializer;

import net.kuujo.copycat.io.Buffer;
import net.kuujo.copycat.io.util.ReferenceCounted;
import net.kuujo.copycat.io.util.ReferenceManager;
import net.kuujo.copycat.io.util.ReferencePool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a special {@link ObjectWriter} implementation that handles serialization for {@link Writable} objects.
 * <p>
 * During deserialization, if the serializable type also implements {@link net.kuujo.copycat.io.util.ReferenceCounted} then the serializer will
 * make an effort to use a {@link net.kuujo.copycat.io.util.ReferencePool} rather than constructing new objects. However, this requires that
 * {@link net.kuujo.copycat.io.util.ReferenceCounted} types provide a single argument {@link net.kuujo.copycat.io.util.ReferenceManager} constructor. If an object is
 * {@link net.kuujo.copycat.io.util.ReferenceCounted} and does not provide a {@link net.kuujo.copycat.io.util.ReferenceManager} constructor then a {@link SerializationException}
 * will be thrown.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
class WritableObjectWriter<T extends Writable> implements ObjectWriter<T> {
  private final Map<Class, ReferencePool> pools = new HashMap<>();
  private final Map<Class, Constructor> constructorMap = new HashMap<>();

  @Override
  public void write(T object, Buffer buffer, Serializer serializer) {
    object.writeObject(buffer, serializer);
  }

  @Override
  public T read(Class<T> type, Buffer buffer, Serializer serializer) {
    if (ReferenceCounted.class.isAssignableFrom(type)) {
      return readReference(type, buffer, serializer);
    } else {
      return readObject(type, buffer, serializer);
    }
  }

  /**
   * Reads an object reference.
   *
   * @param type The reference type.
   * @param buffer The reference buffer.
   * @param serializer The serializer with which the object is being read.
   * @return The reference to read.
   */
  @SuppressWarnings("unchecked")
  private T readReference(Class<T> type, Buffer buffer, Serializer serializer) {
    ReferencePool pool = pools.get(type);
    if (pool == null) {
      Constructor constructor = constructorMap.computeIfAbsent(type, t -> {
        try {
          Constructor c = type.getDeclaredConstructor(ReferenceManager.class);
          c.setAccessible(true);
          return c;
        } catch (NoSuchMethodException e) {
          throw new SerializationException("failed to instantiate reference: must provide a single argument constructor", e);
        }
      });

      pool = new ReferencePool(r -> {
        try {
          return constructor.newInstance(r);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
          throw new SerializationException("failed to instantiate reference", e);
        }
      });
      pools.put(type, pool);
    }
    T object = (T) pool.acquire();
    object.readObject(buffer, serializer);
    return object;
  }

  /**
   * Reads an object.
   *
   * @param type The object type.
   * @param buffer The object buffer.
   * @param serializer The serializer with which the object is being read.
   * @return The object.
   */
  @SuppressWarnings("unchecked")
  private T readObject(Class<T> type, Buffer buffer, Serializer serializer) {
    try {
      Constructor constructor = constructorMap.computeIfAbsent(type, t -> {
        try {
          Constructor c = t.getConstructor();
          c.setAccessible(true);
          return c;
        } catch (NoSuchMethodException e) {
          throw new SerializationException("failed to instantiate object: must provide a no argument constructor", e);
        }
      });

      T object = (T) constructor.newInstance();
      object.readObject(buffer, serializer);
      return object;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new SerializationException("failed to instantiate object: must provide a no argument constructor", e);
    }
  }

}