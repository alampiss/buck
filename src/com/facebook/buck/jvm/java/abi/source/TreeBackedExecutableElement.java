/*
 * Copyright 2017-present Facebook, Inc.
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

package com.facebook.buck.jvm.java.abi.source;

import com.facebook.buck.util.liteinfersupport.Nullable;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * An implementation of {@link ExecutableElement} that uses only the information available from a
 * {@link MethodTree}. This results in an incomplete implementation; see documentation for
 * individual methods and {@link com.facebook.buck.jvm.java.abi.source} for more information.
 */
class TreeBackedExecutableElement extends TreeBackedParameterizable implements ExecutableElement {
  private final ExecutableElement underlyingElement;
  private final MethodTree tree;
  private final List<TreeBackedVariableElement> parameters = new ArrayList<>();

  @Nullable
  private TypeMirror returnType;
  @Nullable
  private List<TypeMirror> thrownTypes;

  TreeBackedExecutableElement(
      ExecutableElement underlyingElement,
      TreeBackedElement enclosingElement,
      TreePath path,
      TreeBackedElementResolver resolver) {
    super(underlyingElement, enclosingElement, path, resolver);
    this.underlyingElement = underlyingElement;
    this.tree = (MethodTree) path.getLeaf();
    enclosingElement.addEnclosedElement(this);
  }

  @Override
  public StandaloneTypeMirror asType() {
    throw new UnsupportedOperationException("NYI");
  }

  @Override
  public TypeMirror getReturnType() {
    if (returnType == null) {
      returnType = getResolver().resolveType(this, tree.getReturnType());
    }
    return returnType;
  }

  @Override
  public List<? extends VariableElement> getParameters() {
    return Collections.unmodifiableList(parameters);
  }

  /* package */ void addParameter(TreeBackedVariableElement parameter) {
    parameters.add(parameter);
  }

  @Override
  public TypeMirror getReceiverType() {
    throw new UnsupportedOperationException("NYI");
  }

  @Override
  public boolean isVarArgs() {
    return underlyingElement.isVarArgs();
  }

  @Override
  public boolean isDefault() {
    return underlyingElement.isDefault();
  }

  @Override
  public List<? extends TypeMirror> getThrownTypes() {
    if (thrownTypes == null) {
      thrownTypes = Collections.unmodifiableList(
          tree.getThrows().stream()
              .map(tree -> getResolver().resolveType(this, tree))
              .collect(Collectors.toList()));
    }

    return thrownTypes;
  }

  @Override
  public AnnotationValue getDefaultValue() {
    throw new UnsupportedOperationException("NYI");
  }
}
