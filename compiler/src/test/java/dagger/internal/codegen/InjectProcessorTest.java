/*
 * Copyright (C) 2014 Google, Inc.
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
package dagger.internal.codegen;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static dagger.internal.codegen.ErrorMessages.ABSTRACT_INJECT_METHOD;
import static dagger.internal.codegen.ErrorMessages.FINAL_INJECT_FIELD;
import static dagger.internal.codegen.ErrorMessages.GENERIC_INJECT_METHOD;
import static dagger.internal.codegen.ErrorMessages.INJECT_CONSTRUCTOR_ON_ABSTRACT_CLASS;
import static dagger.internal.codegen.ErrorMessages.INJECT_CONSTRUCTOR_ON_GENERIC_CLASS;
import static dagger.internal.codegen.ErrorMessages.INJECT_CONSTRUCTOR_ON_INNER_CLASS;
import static dagger.internal.codegen.ErrorMessages.INJECT_ON_PRIVATE_CONSTRUCTOR;
import static dagger.internal.codegen.ErrorMessages.MULTIPLE_INJECT_CONSTRUCTORS;
import static dagger.internal.codegen.ErrorMessages.MULTIPLE_QUALIFIERS;
import static dagger.internal.codegen.ErrorMessages.MULTIPLE_SCOPES;
import static dagger.internal.codegen.ErrorMessages.PRIVATE_INJECT_FIELD;
import static dagger.internal.codegen.ErrorMessages.PRIVATE_INJECT_METHOD;
import static org.truth0.Truth.ASSERT;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public final class InjectProcessorTest {
  private static final JavaFileObject QUALIFIER_A =
      JavaFileObjects.forSourceLines("test.QualifierA",
          "package test;",
          "",
          "import javax.inject.Qualifier;",
          "",
          "@Qualifier @interface QualifierA {}");
  private static final JavaFileObject QUALIFIER_B =
      JavaFileObjects.forSourceLines("test.QualifierB",
          "package test;",
          "",
          "import javax.inject.Qualifier;",
          "",
          "@Qualifier @interface QualifierB {}");
  private static final JavaFileObject SCOPE_A =
      JavaFileObjects.forSourceLines("test.ScopeA",
          "package test;",
          "",
          "import javax.inject.Scope;",
          "",
          "@Scope @interface ScopeA {}");
  private static final JavaFileObject SCOPE_B =
      JavaFileObjects.forSourceLines("test.ScopeB",
          "package test;",
          "",
          "import javax.inject.Scope;",
          "",
          "@Scope @interface ScopeB {}");

  @Test public void injectOnPrivateConstructor() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.PrivateConstructor",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class PrivateConstructor {",
        "  @Inject private PrivateConstructor() {}",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(INJECT_ON_PRIVATE_CONSTRUCTOR)
        .in(file).onLine(6);
  }

  @Test public void injectConstructorOnInnerClass() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.OuterClass",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class OuterClass {",
        "  class InnerClass {",
        "    @Inject InnerClass() {}",
        "  }",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(INJECT_CONSTRUCTOR_ON_INNER_CLASS).in(file).onLine(7);
  }

  @Test public void injectConstructorOnAbstractClass() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.AbstractClass",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "abstract class AbstractClass {",
        "  @Inject AbstractClass() {}",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(INJECT_CONSTRUCTOR_ON_ABSTRACT_CLASS)
        .in(file).onLine(6);
  }

  @Test public void injectConstructorOnGenericClass() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.GenericClass",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class GenericClass<T> {",
        "  @Inject GenericClass() {}",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(INJECT_CONSTRUCTOR_ON_GENERIC_CLASS)
        .in(file).onLine(6);
  }

  @Test public void multipleInjectConstructors() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.TooManyInjectConstructors",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class TooManyInjectConstructors {",
        "  @Inject TooManyInjectConstructors() {}",
        "  TooManyInjectConstructors(int i) {}",
        "  @Inject TooManyInjectConstructors(String s) {}",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(MULTIPLE_INJECT_CONSTRUCTORS).in(file).onLine(6)
        .and().withErrorContaining(MULTIPLE_INJECT_CONSTRUCTORS).in(file).onLine(8);
  }

  @Test public void multipleQualifiersOnInjectConstructorParameter() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.MultipleQualifierConstructorParam",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class MultipleQualifierConstructorParam {",
        "  @Inject MultipleQualifierConstructorParam(@QualifierA @QualifierB String s) {}",
        "}");
    ASSERT.about(javaSources()).that(ImmutableList.of(file, QUALIFIER_A, QUALIFIER_B))
        .processedWith(new InjectProcessor()).failsToCompile()
        // for whatever reason, javac only reports the error once on the constructor
        .withErrorContaining(MULTIPLE_QUALIFIERS).in(file).onLine(6);
  }

  @Test public void injectConstructorOnClassWithMultipleScopes() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.MultipleScopeClass",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "@ScopeA @ScopeB class MultipleScopeClass {",
        "  @Inject MultipleScopeClass() {}",
        "}");
    ASSERT.about(javaSources()).that(ImmutableList.of(file, SCOPE_A, SCOPE_B))
        .processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(MULTIPLE_SCOPES).in(file).onLine(5).atColumn(1)
        .and().withErrorContaining(MULTIPLE_SCOPES).in(file).onLine(5).atColumn(9);
  }

  @Test public void finalInjectField() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.FinalInjectField",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class FinalInjectField {",
        "  @Inject final String s;",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(FINAL_INJECT_FIELD).in(file).onLine(6);
  }

  @Test public void privateInjectField() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.PrivateInjectField",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class PrivateInjectField {",
        "  @Inject private String s;",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(PRIVATE_INJECT_FIELD).in(file).onLine(6);
  }

  @Test public void multipleQualifiersOnField() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.MultipleQualifierInjectField",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class MultipleQualifierInjectField {",
        "  @Inject @QualifierA @QualifierB String s;",
        "}");
    ASSERT.about(javaSources()).that(ImmutableList.of(file, QUALIFIER_A, QUALIFIER_B))
        .processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(MULTIPLE_QUALIFIERS).in(file).onLine(6).atColumn(11)
        .and().withErrorContaining(MULTIPLE_QUALIFIERS).in(file).onLine(6).atColumn(23);
  }

  @Test public void abstractInjectMethod() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.AbstractInjectMethod",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "abstract class AbstractInjectMethod {",
        "  @Inject abstract void method();",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(ABSTRACT_INJECT_METHOD).in(file).onLine(6);
  }

  @Test public void privateInjectMethod() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.PrivateInjectMethod",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class PrivateInjectMethod {",
        "  @Inject private void method();",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(PRIVATE_INJECT_METHOD).in(file).onLine(6);
  }

  @Test public void genericInjectMethod() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.GenericInjectMethod",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class AbstractInjectMethod {",
        "  @Inject <T> void method();",
        "}");
    ASSERT.about(javaSource()).that(file).processedWith(new InjectProcessor()).failsToCompile()
        .withErrorContaining(GENERIC_INJECT_METHOD).in(file).onLine(6);
  }

  @Test public void multipleQualifiersOnInjectMethodParameter() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.MultipleQualifierMethodParam",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class MultipleQualifierMethodParam {",
        "  @Inject void method(@QualifierA @QualifierB String s) {}",
        "}");
    ASSERT.about(javaSources()).that(ImmutableList.of(file, QUALIFIER_A, QUALIFIER_B))
        .processedWith(new InjectProcessor()).failsToCompile()
        // for whatever reason, javac only reports the error once on the method
        .withErrorContaining(MULTIPLE_QUALIFIERS).in(file).onLine(6);
  }
}