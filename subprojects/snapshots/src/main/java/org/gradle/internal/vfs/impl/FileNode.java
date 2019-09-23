/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.vfs.impl;

import org.gradle.internal.snapshot.FileSystemSnapshotVisitor;
import org.gradle.internal.snapshot.RegularFileSnapshot;

import java.util.function.Function;

public class FileNode implements Node {
    private final RegularFileSnapshot snapshot;

    public FileNode(RegularFileSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public Node getChild(String name, Function<Node, Node> nodeSupplier) {
        return new MissingFileNode(this.getChildAbsolutePath(name), name);
    }

    @Override
    public Node getChild(String name, Function<Node, Node> nodeSupplier, Function<Node, Node> replacement) {
        return new MissingFileNode(this.getChildAbsolutePath(name), name);
    }

    @Override
    public String getAbsolutePath() {
        return snapshot.getAbsolutePath();
    }

    @Override
    public Type getType() {
        return Type.FILE;
    }

    @Override
    public void accept(FileSystemSnapshotVisitor visitor) {
        visitor.visitFile(snapshot);
    }
}
