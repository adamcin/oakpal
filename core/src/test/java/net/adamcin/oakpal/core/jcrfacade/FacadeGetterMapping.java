/*
 * Copyright 2019 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.core.jcrfacade;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Function;

import net.adamcin.oakpal.api.Fun;
import org.jetbrains.annotations.NotNull;

public class FacadeGetterMapping {

    @SuppressWarnings("WeakerAccess")
    private static abstract class BaseMapping<W, T, A extends T, F extends W, M extends T, R> {
        protected final @NotNull Class<W> mockType;
        protected final @NotNull Class<M> returnType;
        protected final @NotNull Class<A> assertType;
        protected final @NotNull Function<W, F> facadeFactory;

        protected BaseMapping(final @NotNull Class<W> mockType,
                              final @NotNull Class<M> returnType,
                              final @NotNull Class<A> assertType,
                              final @NotNull Function<W, F> facadeFactory) {
            this.mockType = mockType;
            this.returnType = returnType;
            this.assertType = assertType;
            this.facadeFactory = facadeFactory;
        }

        protected abstract R applyGetter(final @NotNull W toReturn) throws Exception;

        protected abstract R expectMockAsReturn(final @NotNull M toReturn);

        protected abstract T firstFacadeFromReturn(final @NotNull R returnValue);

        public final void doTest() throws Exception {
            final W instance = mock(mockType);
            final M toReturn = mock(returnType);
            when(applyGetter(instance)).thenReturn(expectMockAsReturn(toReturn));
            final F facade = facadeFactory.apply(instance);
            final T fromFacade = firstFacadeFromReturn(applyGetter(facade));
            assertTrue("return facade instance of mapped type", assertType.isInstance(fromFacade));
        }
    }

    private static class SimpleMapping<W, T, A extends T, F extends W, M extends T>
            extends BaseMapping<W, T, A, F, M, T> {
        private final @NotNull Fun.ThrowingFunction<W, T> getter;

        public SimpleMapping(final @NotNull Class<W> mockType,
                             final @NotNull Class<M> returnType,
                             final @NotNull Class<A> assertType,
                             final @NotNull Function<W, F> facadeFactory,
                             final @NotNull Fun.ThrowingFunction<W, T> getter) {
            super(mockType, returnType, assertType, facadeFactory);
            this.getter = getter;
        }

        @Override
        protected T applyGetter(@NotNull final W toReturn) throws Exception {
            return getter.tryApply(toReturn);
        }

        @Override
        protected T expectMockAsReturn(@NotNull final M toReturn) {
            return toReturn;
        }

        @Override
        protected T firstFacadeFromReturn(@NotNull final T returnValue) {
            return returnValue;
        }
    }

    private static class ArrayMapping<W, T, A extends T, F extends W, M extends T>
            extends BaseMapping<W, T, A, F, M, T[]> {
        private final @NotNull Fun.ThrowingFunction<W, T[]> getter;

        public ArrayMapping(final @NotNull Class<W> mockType,
                            final @NotNull Class<M> returnType,
                            final @NotNull Class<A> assertType,
                            final @NotNull Function<W, F> facadeFactory,
                            final @NotNull Fun.ThrowingFunction<W, T[]> getter) {
            super(mockType, returnType, assertType, facadeFactory);
            this.getter = getter;
        }

        @Override
        protected T[] applyGetter(final @NotNull W toReturn) throws Exception {
            return getter.tryApply(toReturn);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T[] expectMockAsReturn(final @NotNull M toReturn) {
            final M[] toReturnArray = (M[]) Array.newInstance(returnType, 1);
            toReturnArray[0] = toReturn;
            return toReturnArray;
        }

        @Override
        protected T firstFacadeFromReturn(final @NotNull T[] returnValue) {
            return returnValue[0];
        }
    }

    private static class IteratorMapping<W, T, A extends T, F extends W, M extends T>
            extends BaseMapping<W, T, A, F, M, Iterator<T>> {
        private final @NotNull Fun.ThrowingFunction<W, Iterator<T>> getter;

        private IteratorMapping(final @NotNull Class<W> mockType,
                               final @NotNull Class<M> returnType,
                               final @NotNull Class<A> assertType,
                               final @NotNull Function<W, F> facadeFactory,
                               final @NotNull Fun.ThrowingFunction<W, Iterator<T>> getter) {
            super(mockType, returnType, assertType, facadeFactory);
            this.getter = getter;
        }

        @Override
        protected Iterator<T> applyGetter(@NotNull final W toReturn) throws Exception {
            return getter.tryApply(toReturn);
        }

        @Override
        protected Iterator<T> expectMockAsReturn(@NotNull final M toReturn) {
            return Collections.singletonList((T) toReturn).iterator();
        }

        @Override
        protected T firstFacadeFromReturn(final @NotNull Iterator<T> returnValue) {
            return returnValue.next();
        }
    }

    public static class Tester<W, F extends W> {
        private final Class<W> mockType;
        private final Function<W, F> facadeFactory;

        public Tester(final @NotNull Class<W> mockType, final @NotNull Function<W, F> facadeFactory) {
            this.mockType = mockType;
            this.facadeFactory = facadeFactory;
        }

        public <T, A extends T, M extends T> Tester<W, F>
        testFacadeGetter(final @NotNull Class<M> returnType,
                         final @NotNull Class<A> assertType,
                         final @NotNull Fun.ThrowingFunction<W, T> getter) throws Exception {
            SimpleMapping<W, T, A, F, M> mapping =
                    new SimpleMapping<>(mockType, returnType, assertType, facadeFactory, getter);
            mapping.doTest();
            return this;
        }

        public <T, A extends T, M extends T> Tester<W, F>
        testFacadeArrayGetter(final @NotNull Class<M> returnType,
                              final @NotNull Class<A> assertType,
                              final @NotNull Fun.ThrowingFunction<W, T[]> arrayGetter) throws Exception {

            ArrayMapping<W, T, A, F, M> mapping =
                    new ArrayMapping<>(mockType, returnType, assertType, facadeFactory, arrayGetter);
            mapping.doTest();
            return this;
        }

        public <T, A extends T, M extends T> Tester<W, F>
        testFacadeIteratorGetter(final @NotNull Class<M> returnType,
                                 final @NotNull Class<A> assertType,
                                 final @NotNull Fun.ThrowingFunction<W, Iterator<T>> iteratorGetter) throws Exception {

            IteratorMapping<W, T, A, F, M> mapping =
                    new IteratorMapping<>(mockType, returnType, assertType, facadeFactory, iteratorGetter);
            mapping.doTest();
            return this;
        }
    }
}
