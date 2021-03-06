/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;

import org.hamcrest.Matchers;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/** Base class for tests that want to use the Compiler Tree API exposed by javac. */
public abstract class CompilerTreeApiTest {
  public interface TaskListenerFactory {
    TaskListener newTaskListener(JavacTask task);
  }

  @Rule
  public TestCompiler testCompiler = new TestCompiler();

  protected Elements elements;
  protected Trees trees;
  protected Types types;

  protected boolean useFrontendOnlyJavacTask() {
    return false;
  }

  protected final void initCompiler() throws IOException {
    initCompiler(Collections.emptyMap());
  }

  protected void initCompiler(
      Map<String, String> fileNamesToContents) throws IOException {

    if (useFrontendOnlyJavacTask()) {
      testCompiler.useFrontendOnlyJavacTask();
    }
    for (Map.Entry<String, String> fileNameToContents : fileNamesToContents.entrySet()) {
      testCompiler.addSourceFileContents(
          fileNameToContents.getKey(),
          fileNameToContents.getValue());
    }

    trees = testCompiler.getTrees();
    elements = testCompiler.getElements();
    types = testCompiler.getTypes();
  }

  protected final Iterable<? extends CompilationUnitTree> compile(String source)
      throws IOException {
    return compile(ImmutableMap.of("Foo.java", source));
  }

  protected final Iterable<? extends CompilationUnitTree> compile(Map<String, String> sources)
      throws IOException {
    return compile(sources, null);
  }

  protected final Iterable<? extends CompilationUnitTree> compile(
      Map<String, String> fileNamesToContents,
      TaskListenerFactory taskListenerFactory) throws IOException {

    initCompiler(fileNamesToContents);

    if (taskListenerFactory != null) {
      testCompiler.setTaskListener(
          taskListenerFactory.newTaskListener(testCompiler.getJavacTask()));
    }

    // Suppress processor auto-discovery; it was picking up the immutables processor unnecessarily
    testCompiler.setProcessors(Collections.emptyList());

    final Iterable<? extends CompilationUnitTree> compilationUnits = testCompiler.parse();

    // Make sure we've got elements for things.
    testCompiler.enter();

    return compilationUnits;
  }

  protected void withClasspath(
      Map<String, String> fileNamesToContents) throws IOException {

    for (Map.Entry<String, String> fileNameToContents : fileNamesToContents.entrySet()) {
      testCompiler.addClasspathFileContents(
          fileNameToContents.getKey(),
          fileNameToContents.getValue());
    }
  }

  protected TypeMirror getTypeParameterUpperBound(String typeName, int typeParameterIndex) {
    TypeParameterElement typeParameter =
        elements.getTypeElement(typeName).getTypeParameters().get(typeParameterIndex);
    TypeVariable typeVariable = (TypeVariable) typeParameter.asType();

    return typeVariable.getUpperBound();
  }

  protected ExecutableElement findMethod(String name, TypeElement typeElement) {
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() == ElementKind.METHOD && element.getSimpleName().contentEquals(name)) {
        return (ExecutableElement) element;
      }
    }

    throw new IllegalArgumentException(String.format(
        "No such method in %s: %s",
        typeElement.getQualifiedName(),
        name));
  }

  protected VariableElement findField(String name, TypeElement typeElement) {
    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind().isField() && element.getSimpleName().contentEquals(name)) {
        return (VariableElement) element;
      }
    }

    throw new IllegalArgumentException(String.format(
        "No such field in %s: %s",
        typeElement.getQualifiedName(),
        name));
  }

  protected VariableElement findParameter(String name, ExecutableElement method) {
    for (VariableElement parameter : method.getParameters()) {
      if (parameter.getSimpleName().contentEquals(name)) {
        return parameter;
      }
    }

    throw new IllegalArgumentException(String.format(
        "No such parameter on %s: %s",
        method.getSimpleName(),
        name));
  }

  protected void assertNameEquals(String expected, Name actual) {
    assertEquals(elements.getName(expected), actual);
  }

  protected void assertSameType(TypeMirror expected, TypeMirror actual) {
    if (!types.isSameType(expected, actual)) {
      fail(String.format("Types are not the same.\nExpected: %s\nActual: %s", expected, actual));
    }
  }

  protected void assertNotSameType(TypeMirror expected, TypeMirror actual) {
    if (types.isSameType(expected, actual)) {
      fail(String.format("Expected different types, but both were: %s", expected));
    }
  }
  protected void assertNoErrors() {
    assertThat(testCompiler.getDiagnostics(), Matchers.empty());
  }

  protected void assertError(String message) {
    assertErrors(message);
  }

  protected void assertErrors(String... messages) {
    assertThat(
        testCompiler.getDiagnostics()
            .stream()
            .map(diagnostic -> {
              String toString = diagnostic.toString();
              return toString.substring(toString.lastIndexOf(File.separatorChar) + 1);
            })
            .collect(Collectors.toSet()),
        Matchers.containsInAnyOrder(messages));
  }

}
