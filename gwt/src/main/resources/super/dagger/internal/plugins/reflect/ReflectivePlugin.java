/*
 * Copyright (C) 2012 Square, Inc.
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
package dagger.internal.plugins.reflect;

import dagger.internal.Binding;
import dagger.internal.ModuleAdapter;
import dagger.internal.Plugin;
import dagger.internal.StaticInjection;

/**
 * This is a stub.
 */
public final class ReflectivePlugin implements Plugin {
  @Override public Binding<?> getAtInjectBinding(
      String key, String className, boolean mustBeInjectable) {
    throw new UnsupportedOperationException();
  }

  @Override public <T> ModuleAdapter<T> getModuleAdapter(Class<? extends T> moduleClass, T module) {
    throw new UnsupportedOperationException();
  }

  @Override public StaticInjection getStaticInjection(Class<?> injectedClass) {
    throw new UnsupportedOperationException();
  }
}